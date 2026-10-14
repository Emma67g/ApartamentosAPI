package com.blue.apartamentos.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blue.apartamentos.models.ClienteModel;
import com.blue.apartamentos.models.PagoModel;
import com.blue.apartamentos.models.PagoModel.EstadoPago;
import com.blue.apartamentos.models.PagoModel.MetodoPago;
import com.blue.apartamentos.models.ReservaModel;
import com.blue.apartamentos.models.ReservaModel.EstadoReserva;
import com.blue.apartamentos.repositories.IClienteRepository;
import com.blue.apartamentos.repositories.IPagoRepository;
import com.blue.apartamentos.repositories.IReservaRepository;

@Service
@Transactional(readOnly = true)
public class PagoService {

    /** Estado de cuenta de un cliente: lo facturado en sus reservas activas, lo pagado y lo que adeuda. */
    public record EstadoCuenta(
            Long clienteId,
            String clienteNombre,
            LocalDate fechaCorte,
            BigDecimal totalFacturado,
            BigDecimal totalPagado,
            BigDecimal saldoPendiente,
            /** Parte del saldo en cuotas PENDIENTES que ya vencieron. */
            BigDecimal montoVencido,
            List<ReservaCuenta> reservas) {
    }

    public record ReservaCuenta(
            Long reservaId,
            String propiedadTitulo,
            String habitacionNumero,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoReserva estado,
            BigDecimal montoTotal,
            BigDecimal montoPagado,
            BigDecimal saldoPendiente,
            BigDecimal montoVencido,
            List<PagoModel> pagos) {
    }

    private static final long SIN_EXCLUSION = 0L;

    private final IPagoRepository pagoRepository;
    private final IReservaRepository reservaRepository;
    private final IClienteRepository clienteRepository;

    public PagoService(IPagoRepository pagoRepository, IReservaRepository reservaRepository,
            IClienteRepository clienteRepository) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
    }

    /** @param soloVencidos si es true, devuelve únicamente pagos PENDIENTES con vencimiento ya pasado */
    public List<PagoModel> listar(Long reservaId, Long clienteId, EstadoPago estado, boolean soloVencidos) {
        LocalDate hoy = LocalDate.now();
        return pagoRepository.buscar(reservaId, clienteId, estado).stream()
                .filter(p -> !soloVencidos || p.estaVencido(hoy))
                .toList();
    }

    public PagoModel obtener(Long id) {
        return buscar(id);
    }

    /** Programa un cargo (cuota) PENDIENTE de una reserva. */
    @Transactional
    public PagoModel crear(PagoModel datos) {
        Long reservaId = Validaciones.exigirId(datos.getReserva().getId(), "reserva.id");
        ReservaModel reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ConflictoException("No se pueden programar pagos en una reserva cancelada.");
        }
        validarMontoProgramable(reserva, datos.getMonto(), SIN_EXCLUSION);

        PagoModel pago = new PagoModel();
        pago.setReserva(reserva);
        pago.setMonto(datos.getMonto());
        pago.setFechaVencimiento(datos.getFechaVencimiento());
        pago.setReferencia(Validaciones.textoONull(datos.getReferencia()));
        pago.setEstado(EstadoPago.PENDIENTE);
        return pagoRepository.save(pago);
    }

    /** Modifica un cargo que todavía está PENDIENTE (monto, vencimiento o referencia). */
    @Transactional
    public PagoModel actualizar(Long id, PagoModel datos) {
        PagoModel pago = buscar(id);
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new ConflictoException(
                    "Solo se pueden modificar pagos PENDIENTES (estado actual: " + pago.getEstado() + ").");
        }
        Long reservaId = Validaciones.exigirId(datos.getReserva().getId(), "reserva.id");
        if (!pago.getReserva().getId().equals(reservaId)) {
            throw new SolicitudInvalidaException("Un pago no puede cambiarse a otra reserva.");
        }
        validarMontoProgramable(pago.getReserva(), datos.getMonto(), pago.getId());
        pago.setMonto(datos.getMonto());
        pago.setFechaVencimiento(datos.getFechaVencimiento());
        pago.setReferencia(Validaciones.textoONull(datos.getReferencia()));
        return pagoRepository.save(pago);
    }

    /** Liquida (PAGADO, con método de pago) o cancela un cargo. */
    @Transactional
    public PagoModel cambiarEstado(Long id, EstadoPago nuevoEstado, MetodoPago metodoPago, String referencia) {
        if (nuevoEstado == null) {
            throw new SolicitudInvalidaException("Debe indicar el estado (PAGADO o CANCELADO).");
        }
        PagoModel pago = buscar(id);
        if (!pago.getEstado().puedePasarA(nuevoEstado)) {
            throw new ConflictoException(
                    "No se puede pasar un pago de " + pago.getEstado() + " a " + nuevoEstado + ".");
        }
        if (nuevoEstado == EstadoPago.PAGADO) {
            MetodoPago metodo = metodoPago != null ? metodoPago : pago.getMetodoPago();
            if (metodo == null) {
                throw new SolicitudInvalidaException(
                        "Para registrar un pago como PAGADO debe indicar el método de pago.");
            }
            pago.setMetodoPago(metodo);
            pago.setFechaPago(LocalDate.now());
        }
        if (referencia != null) {
            pago.setReferencia(referencia);
        }
        pago.setEstado(nuevoEstado);
        return pagoRepository.save(pago);
    }

    @Transactional
    public void eliminar(Long id) {
        PagoModel pago = buscar(id);
        if (pago.getEstado() == EstadoPago.PAGADO) {
            throw new ConflictoException("Un pago ya PAGADO es un registro contable y no puede eliminarse.");
        }
        pagoRepository.delete(pago);
    }

    /** Estado de cuenta: por cada reserva activa del cliente, lo facturado, lo pagado, el saldo y lo vencido. */
    public EstadoCuenta estadoCuenta(Long clienteId) {
        ClienteModel cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));
        LocalDate hoy = LocalDate.now();

        List<ReservaModel> reservas = reservaRepository
                .findByCliente_IdAndEstadoNotOrderByFechaInicioDesc(clienteId, EstadoReserva.CANCELADA);
        Map<Long, List<PagoModel>> pagosPorReserva = pagoRepository.findByReserva_Cliente_Id(clienteId).stream()
                .collect(Collectors.groupingBy(p -> p.getReserva().getId()));

        List<ReservaCuenta> detalle = reservas.stream().map(reserva -> {
            List<PagoModel> pagos = pagosPorReserva.getOrDefault(reserva.getId(), List.of());
            BigDecimal pagado = sumar(pagos, p -> p.getEstado() == EstadoPago.PAGADO);
            BigDecimal vencido = sumar(pagos, p -> p.estaVencido(hoy));
            return new ReservaCuenta(reserva.getId(), reserva.getPropiedad().getTitulo(),
                    reserva.getHabitacion() != null ? reserva.getHabitacion().getNumero() : null,
                    reserva.getFechaInicio(), reserva.getFechaFin(), reserva.getEstado(), reserva.getMontoTotal(),
                    pagado, reserva.getMontoTotal().subtract(pagado), vencido, pagos);
        }).toList();

        BigDecimal facturado = detalle.stream().map(ReservaCuenta::montoTotal).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal pagado = detalle.stream().map(ReservaCuenta::montoPagado).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal vencido = detalle.stream().map(ReservaCuenta::montoVencido).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        return new EstadoCuenta(clienteId, cliente.getNombreCompleto(), hoy, facturado, pagado,
                facturado.subtract(pagado), vencido, detalle);
    }

    private PagoModel buscar(Long id) {
        return pagoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Pago", id));
    }

    /** Lo programado en cargos no cancelados (incluido el nuevo) no puede superar el monto de la reserva. */
    private void validarMontoProgramable(ReservaModel reserva, BigDecimal monto, long excluirPagoId) {
        BigDecimal yaProgramado = pagoRepository.findByReserva_Id(reserva.getId()).stream()
                .filter(p -> p.getEstado() != EstadoPago.CANCELADO)
                .filter(p -> !p.getId().equals(excluirPagoId))
                .map(PagoModel::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disponible = reserva.getMontoTotal().subtract(yaProgramado);
        if (monto.compareTo(disponible) > 0) {
            throw new ConflictoException("El monto (" + monto + ") excede lo que falta por programar de la reserva ("
                    + disponible + " de " + reserva.getMontoTotal() + ").");
        }
    }

    private BigDecimal sumar(List<PagoModel> pagos, Predicate<PagoModel> filtro) {
        return pagos.stream().filter(filtro).map(PagoModel::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
