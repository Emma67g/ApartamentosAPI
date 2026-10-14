package com.blue.apartamentos.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blue.apartamentos.models.DisponibilidadModel;
import com.blue.apartamentos.models.HabitacionModel;
import com.blue.apartamentos.models.HabitacionModel.EstadoHabitacion;
import com.blue.apartamentos.models.PropiedadModel;
import com.blue.apartamentos.models.PropiedadModel.EstadoPropiedad;
import com.blue.apartamentos.repositories.IDisponibilidadRepository;
import com.blue.apartamentos.repositories.IHabitacionRepository;
import com.blue.apartamentos.repositories.IPropiedadRepository;
import com.blue.apartamentos.repositories.IReservaRepository;

/**
 * Calendario de las unidades rentables: bloqueos, tarifas especiales y la regla central
 * "¿se puede reservar esta unidad en estas fechas?" que también usa ReservaService.
 */
@Service
@Transactional(readOnly = true)
public class DisponibilidadService {

    /** Resultado de consultar si una propiedad o habitación está libre en un rango de fechas. */
    public record Consulta(
            Long propiedadId,
            Long habitacionId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            long noches,
            boolean disponible,
            /** Si no está disponible explica por qué; si lo está es null. */
            String motivo,
            /** Monto total estimado de la estancia (considera tarifas especiales); null si no está disponible. */
            BigDecimal montoEstimado) {
    }

    private final IDisponibilidadRepository disponibilidadRepository;
    private final IPropiedadRepository propiedadRepository;
    private final IHabitacionRepository habitacionRepository;
    private final IReservaRepository reservaRepository;

    public DisponibilidadService(IDisponibilidadRepository disponibilidadRepository,
            IPropiedadRepository propiedadRepository, IHabitacionRepository habitacionRepository,
            IReservaRepository reservaRepository) {
        this.disponibilidadRepository = disponibilidadRepository;
        this.propiedadRepository = propiedadRepository;
        this.habitacionRepository = habitacionRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<DisponibilidadModel> listar(Long propiedadId, Long habitacionId) {
        return disponibilidadRepository.buscar(propiedadId, habitacionId);
    }

    public DisponibilidadModel obtener(Long id) {
        return buscar(id);
    }

    public Consulta consultar(Long propiedadId, Long habitacionId, LocalDate inicio, LocalDate fin) {
        Validaciones.rangoDeFechas(inicio, fin);
        PropiedadModel propiedad = buscarPropiedad(propiedadId);
        HabitacionModel habitacion = resolverHabitacion(habitacionId, propiedad);

        Optional<String> motivo = motivoNoDisponible(propiedad, habitacion, inicio, fin,
                IReservaRepository.SIN_EXCLUSION);
        BigDecimal monto = motivo.isEmpty() ? calcularMontoEstancia(propiedad, habitacion, inicio, fin) : null;
        return new Consulta(propiedadId, habitacionId, inicio, fin, ChronoUnit.DAYS.between(inicio, fin),
                motivo.isEmpty(), motivo.orElse(null), monto);
    }

    @Transactional
    public DisponibilidadModel crear(DisponibilidadModel datos) {
        Validaciones.rangoDeFechas(datos.getFechaInicio(), datos.getFechaFin());
        PropiedadModel propiedad = buscarPropiedad(Validaciones.exigirId(datos.getPropiedad().getId(), "propiedad.id"));
        HabitacionModel habitacion = resolverHabitacion(idHabitacion(datos), propiedad);

        boolean disponible = Boolean.TRUE.equals(datos.getDisponible());
        if (disponible && datos.getPrecioEspecial() == null) {
            throw new SolicitudInvalidaException(
                    "Un rango disponible sirve para definir una tarifa especial: indique precioEspecial "
                            + "(o use disponible=false para bloquear fechas).");
        }
        if (!disponible && datos.getPrecioEspecial() != null) {
            throw new SolicitudInvalidaException("Un bloqueo (disponible=false) no puede tener precioEspecial.");
        }
        if (!disponible && hayReservas(propiedad, habitacion, datos.getFechaInicio(), datos.getFechaFin())) {
            throw new ConflictoException("No se pueden bloquear esas fechas: ya existen reservas activas en el rango.");
        }

        DisponibilidadModel d = new DisponibilidadModel();
        d.setPropiedad(propiedad);
        d.setHabitacion(habitacion);
        d.setFechaInicio(datos.getFechaInicio());
        d.setFechaFin(datos.getFechaFin());
        d.setDisponible(disponible);
        d.setPrecioEspecial(datos.getPrecioEspecial());
        d.setMotivo(Validaciones.textoONull(datos.getMotivo()));
        return disponibilidadRepository.save(d);
    }

    @Transactional
    public void eliminar(Long id) {
        disponibilidadRepository.delete(buscar(id));
    }

    /**
     * Devuelve por qué la unidad NO se puede reservar en [inicio, fin), o vacío si está libre.
     * Reglas: la propiedad/habitación debe estar operativa, sin bloqueos y sin reservas activas que se crucen.
     * Reservar la propiedad completa choca con reservas y bloqueos de cualquiera de sus habitaciones, y
     * reservar una habitación choca con las de la propiedad completa.
    */
    public Optional<String> motivoNoDisponible(PropiedadModel propiedad, HabitacionModel habitacion,
            LocalDate inicio, LocalDate fin, Long excluirReservaId) {
        if (propiedad.getEstado() != EstadoPropiedad.DISPONIBLE) {
            return Optional.of("La propiedad no está disponible (estado " + propiedad.getEstado() + ").");
        }
        if (habitacion != null && (habitacion.getEstado() == EstadoHabitacion.NO_DISPONIBLE
                || habitacion.getEstado() == EstadoHabitacion.MANTENIMIENTO)) {
            return Optional.of("La habitación no está disponible (estado " + habitacion.getEstado() + ").");
        }
        long bloqueos = habitacion == null
                ? disponibilidadRepository.contarBloqueosPropiedad(propiedad.getId(), inicio, fin)
                : disponibilidadRepository.contarBloqueosHabitacion(propiedad.getId(), habitacion.getId(), inicio,
                        fin);
        if (bloqueos > 0) {
            return Optional.of("Las fechas solicitadas están bloqueadas en el calendario de disponibilidad.");
        }
        boolean hayReserva = habitacion == null
                ? reservaRepository.hayReservaEnPropiedad(propiedad.getId(), inicio, fin, excluirReservaId)
                : reservaRepository.hayReservaEnHabitacion(propiedad.getId(), habitacion.getId(), inicio, fin,
                        excluirReservaId);
        if (hayReserva) {
            return Optional.of("Ya existe una reserva activa que se cruza con las fechas solicitadas.");
        }
        return Optional.empty();
    }

    /**
     * Monto de la estancia noche por noche: precio base de la unidad, o la tarifa especial vigente
     * esa noche (si hay varias que se cruzan, gana la registrada más recientemente).
     */
    public BigDecimal calcularMontoEstancia(PropiedadModel propiedad, HabitacionModel habitacion, LocalDate inicio,
            LocalDate fin) {
        BigDecimal precioBase = habitacion != null ? habitacion.getPrecioNoche() : propiedad.getPrecioNoche();
        List<DisponibilidadModel> tarifas = habitacion != null
                ? disponibilidadRepository.findTarifasEspecialesHabitacion(habitacion.getId(), inicio, fin)
                : disponibilidadRepository.findTarifasEspecialesPropiedad(propiedad.getId(), inicio, fin);

        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate cursor = inicio; cursor.isBefore(fin); cursor = cursor.plusDays(1)) {
            LocalDate noche = cursor;
            BigDecimal precioNoche = tarifas.stream()
                    .filter(t -> !noche.isBefore(t.getFechaInicio()) && noche.isBefore(t.getFechaFin()))
                    .findFirst()
                    .map(DisponibilidadModel::getPrecioEspecial)
                    .orElse(precioBase);
            total = total.add(precioNoche);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** Devuelve la habitación (verificando que pertenezca a la propiedad) o null si no se indicó habitacionId. */
    public HabitacionModel resolverHabitacion(Long habitacionId, PropiedadModel propiedad) {
        if (habitacionId == null) {
            return null;
        }
        HabitacionModel habitacion = habitacionRepository.findById(habitacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitación", habitacionId));
        if (!habitacion.getPropiedad().getId().equals(propiedad.getId())) {
            throw new SolicitudInvalidaException(
                    "La habitación " + habitacionId + " no pertenece a la propiedad " + propiedad.getId() + ".");
        }
        return habitacion;
    }

    private Long idHabitacion(DisponibilidadModel datos) {
        return datos.getHabitacion() == null ? null
                : Validaciones.exigirId(datos.getHabitacion().getId(), "habitacion.id");
    }

    private boolean hayReservas(PropiedadModel propiedad, HabitacionModel habitacion, LocalDate inicio,
            LocalDate fin) {
        return habitacion == null
                ? reservaRepository.hayReservaEnPropiedad(propiedad.getId(), inicio, fin,
                        IReservaRepository.SIN_EXCLUSION)
                : reservaRepository.hayReservaEnHabitacion(propiedad.getId(), habitacion.getId(), inicio, fin,
                        IReservaRepository.SIN_EXCLUSION);
    }

    private DisponibilidadModel buscar(Long id) {
        return disponibilidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Disponibilidad", id));
    }

    private PropiedadModel buscarPropiedad(Long id) {
        return propiedadRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Propiedad", id));
    }
}
