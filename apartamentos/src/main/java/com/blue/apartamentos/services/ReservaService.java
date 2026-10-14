package com.blue.apartamentos.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blue.apartamentos.models.ClienteModel;
import com.blue.apartamentos.models.ClienteModel.ClienteStatus;
import com.blue.apartamentos.models.HabitacionModel;
import com.blue.apartamentos.models.PagoModel;
import com.blue.apartamentos.models.PagoModel.EstadoPago;
import com.blue.apartamentos.models.PropiedadModel;
import com.blue.apartamentos.models.ReservaModel;
import com.blue.apartamentos.models.ReservaModel.EstadoReserva;
import com.blue.apartamentos.repositories.IClienteRepository;
import com.blue.apartamentos.repositories.IPagoRepository;
import com.blue.apartamentos.repositories.IPropiedadRepository;
import com.blue.apartamentos.repositories.IReservaRepository;

@Service
@Transactional(readOnly = true)
public class ReservaService {

    private final IReservaRepository reservaRepository;
    private final IClienteRepository clienteRepository;
    private final IPropiedadRepository propiedadRepository;
    private final IPagoRepository pagoRepository;
    private final DisponibilidadService disponibilidadService;

    public ReservaService(IReservaRepository reservaRepository, IClienteRepository clienteRepository,
            IPropiedadRepository propiedadRepository, IPagoRepository pagoRepository,
            DisponibilidadService disponibilidadService) {
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
        this.propiedadRepository = propiedadRepository;
        this.pagoRepository = pagoRepository;
        this.disponibilidadService = disponibilidadService;
    }

    public List<ReservaModel> listar(Long clienteId, Long propiedadId, Long habitacionId, EstadoReserva estado) {
        return reservaRepository.buscar(clienteId, propiedadId, habitacionId, estado);
    }

    public ReservaModel obtener(Long id) {
        return buscar(id);
    }

    @Transactional
    public ReservaModel crear(ReservaModel datos) {
        Validaciones.rangoDeFechas(datos.getFechaInicio(), datos.getFechaFin());
        if (datos.getFechaInicio().isBefore(LocalDate.now())) {
            throw new SolicitudInvalidaException("La fecha de inicio no puede estar en el pasado.");
        }

        Long clienteId = Validaciones.exigirId(datos.getCliente().getId(), "cliente.id");
        ClienteModel cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));
        if (cliente.getEstatus() != ClienteStatus.ACTIVO) {
            throw new ConflictoException(
                    "El cliente está " + cliente.getEstatus() + " y no puede realizar reservas.");
        }
        Long propiedadId = Validaciones.exigirId(datos.getPropiedad().getId(), "propiedad.id");
        PropiedadModel propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propiedad", propiedadId));
        HabitacionModel habitacion = disponibilidadService.resolverHabitacion(idHabitacion(datos), propiedad);

        validarCapacidad(datos.getNumHuespedes(), propiedad, habitacion);
        disponibilidadService
                .motivoNoDisponible(propiedad, habitacion, datos.getFechaInicio(), datos.getFechaFin(),
                        IReservaRepository.SIN_EXCLUSION)
                .ifPresent(motivo -> {
                    throw new ConflictoException(motivo);
                });

        ReservaModel reserva = new ReservaModel();
        reserva.setCliente(cliente);
        reserva.setPropiedad(propiedad);
        reserva.setHabitacion(habitacion);
        reserva.setFechaInicio(datos.getFechaInicio());
        reserva.setFechaFin(datos.getFechaFin());
        reserva.setNumHuespedes(datos.getNumHuespedes());
        reserva.setNotas(Validaciones.textoONull(datos.getNotas()));
        reserva.setEstado(EstadoReserva.PENDIENTE);
        reserva.setMontoTotal(disponibilidadService.calcularMontoEstancia(propiedad, habitacion,
                datos.getFechaInicio(), datos.getFechaFin()));
        return reservaRepository.save(reserva);
    }

    /** Cambia fechas, huéspedes o notas; el cliente y la unidad reservada no pueden cambiar. */
    @Transactional
    public ReservaModel actualizar(Long id, ReservaModel datos) {
        ReservaModel reserva = buscar(id);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE && reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ConflictoException("Solo se pueden modificar reservas PENDIENTES o CONFIRMADAS (estado actual: "
                    + reserva.getEstado() + ").");
        }
        verificarMismaUnidad(reserva, datos);
        Validaciones.rangoDeFechas(datos.getFechaInicio(), datos.getFechaFin());
        if (!datos.getFechaInicio().equals(reserva.getFechaInicio())
                && datos.getFechaInicio().isBefore(LocalDate.now())) {
            throw new SolicitudInvalidaException("La nueva fecha de inicio no puede estar en el pasado.");
        }

        PropiedadModel propiedad = reserva.getPropiedad();
        HabitacionModel habitacion = reserva.getHabitacion();
        validarCapacidad(datos.getNumHuespedes(), propiedad, habitacion);
        disponibilidadService
                .motivoNoDisponible(propiedad, habitacion, datos.getFechaInicio(), datos.getFechaFin(),
                        reserva.getId())
                .ifPresent(motivo -> {
                    throw new ConflictoException(motivo);
                });

        BigDecimal nuevoMonto = disponibilidadService.calcularMontoEstancia(propiedad, habitacion,
                datos.getFechaInicio(), datos.getFechaFin());
        BigDecimal programado = pagoRepository.findByReserva_Id(id).stream()
                .filter(p -> p.getEstado() != EstadoPago.CANCELADO)
                .map(PagoModel::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (programado.compareTo(nuevoMonto) > 0) {
            throw new ConflictoException("El nuevo monto de la reserva (" + nuevoMonto + ") es menor a los pagos ya "
                    + "programados (" + programado + "). Cancele o ajuste esos pagos primero.");
        }

        reserva.setFechaInicio(datos.getFechaInicio());
        reserva.setFechaFin(datos.getFechaFin());
        reserva.setNumHuespedes(datos.getNumHuespedes());
        reserva.setNotas(Validaciones.textoONull(datos.getNotas()));
        reserva.setMontoTotal(nuevoMonto);
        return reservaRepository.save(reserva);
    }

    /** PATCH: del cuerpo solo se usa el campo estado. */
    @Transactional
    public ReservaModel cambiarEstado(Long id, EstadoReserva nuevoEstado) {
        if (nuevoEstado == null) {
            throw new SolicitudInvalidaException(
                    "Debe indicar el estado (PENDIENTE, CONFIRMADA, CANCELADA o COMPLETADA).");
        }
        ReservaModel reserva = buscar(id);
        if (!reserva.getEstado().puedePasarA(nuevoEstado)) {
            throw new ConflictoException(
                    "No se puede pasar una reserva de " + reserva.getEstado() + " a " + nuevoEstado + ".");
        }
        reserva.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoReserva.CANCELADA) {
            // Los cargos que aún no se pagaron dejan de ser exigibles (y de contar como morosidad).
            pagoRepository.findByReserva_IdAndEstado(id, EstadoPago.PENDIENTE)
                    .forEach(pago -> pago.setEstado(EstadoPago.CANCELADO));
        }
        return reservaRepository.save(reserva);
    }

    @Transactional
    public void eliminar(Long id) {
        ReservaModel reserva = buscar(id);
        if (pagoRepository.existsByReserva_Id(id)) {
            throw new ConflictoException(
                    "La reserva tiene pagos registrados y no puede eliminarse. Cancélela cambiando su estado.");
        }
        reservaRepository.delete(reserva);
    }

    private ReservaModel buscar(Long id) {
        return reservaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
    }

    private Long idHabitacion(ReservaModel datos) {
        return datos.getHabitacion() == null ? null
                : Validaciones.exigirId(datos.getHabitacion().getId(), "habitacion.id");
    }

    private void verificarMismaUnidad(ReservaModel reserva, ReservaModel datos) {
        Long habitacionActual = reserva.getHabitacion() == null ? null : reserva.getHabitacion().getId();
        boolean igual = reserva.getCliente().getId().equals(datos.getCliente().getId())
                && reserva.getPropiedad().getId().equals(datos.getPropiedad().getId())
                && Objects.equals(habitacionActual, idHabitacion(datos));
        if (!igual) {
            throw new SolicitudInvalidaException("El cliente, la propiedad y la habitación de una reserva no pueden "
                    + "modificarse: cancele la reserva y cree una nueva.");
        }
    }

    private void validarCapacidad(int numHuespedes, PropiedadModel propiedad, HabitacionModel habitacion) {
        int capacidadMaxima = habitacion != null ? habitacion.getCapacidad() : propiedad.getCapacidad();
        if (numHuespedes > capacidadMaxima) {
            throw new SolicitudInvalidaException("El número de huéspedes (" + numHuespedes
                    + ") excede la capacidad de la unidad reservada (" + capacidadMaxima + ").");
        }
    }
}
