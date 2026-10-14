package com.blue.apartamentos.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blue.apartamentos.models.ClienteModel;
import com.blue.apartamentos.models.PagoModel;
import com.blue.apartamentos.models.PagoModel.EstadoPago;
import com.blue.apartamentos.models.PagoModel.MetodoPago;
import com.blue.apartamentos.models.PropiedadModel;
import com.blue.apartamentos.models.ReservaModel;
import com.blue.apartamentos.models.ReservaModel.EstadoReserva;
import com.blue.apartamentos.repositories.IHabitacionRepository;
import com.blue.apartamentos.repositories.IPagoRepository;
import com.blue.apartamentos.repositories.IPropiedadRepository;
import com.blue.apartamentos.repositories.IReservaRepository;

@Service
@Transactional(readOnly = true)
public class ReporteService {

    /**
     * Ocupación de una propiedad en [desde, hasta). Se mide en "noches-unidad": una propiedad con N habitaciones
     * registradas ofrece N unidades por noche (1 si no tiene habitaciones registradas); reservarla completa ocupa
     * todas sus unidades y reservar una habitación ocupa una.
     */
    public record Ocupacion(
            Long idPropiedad,
            String tituloPropiedad,
            LocalDate desde,
            LocalDate hasta,
            long unidadesAlquilables,
            long nochesReservadas,
            long nochesTotales,
            double porcentajeOcupacion) {
    }

    /** Ingresos = pagos PAGADOS cuya fecha de pago cae en [desde, hasta]. */
    public record Ingresos(
            LocalDate desde,
            LocalDate hasta,
            Long idPropiedad,
            BigDecimal totalIngresos,
            int cantidadPagos,
            Map<MetodoPago, BigDecimal> ingresosPorMetodoPago) {
    }

    /** Cliente con al menos un pago PENDIENTE cuya fecha de vencimiento ya pasó. */
    public record Moroso(
            Long idCliente,
            String nombreCliente,
            String email,
            String telefono,
            BigDecimal montoAdeudado,
            int cantidadPagosVencidos,
            LocalDate vencimientoMasAntiguo,
            long diasMaximoVencido) {
    }

    /** Solo cuentan como ocupación las reservas confirmadas o ya completadas. */
    private static final List<EstadoReserva> ESTADOS_OCUPADOS = List.of(EstadoReserva.CONFIRMADA,
            EstadoReserva.COMPLETADA);

    private final IReservaRepository reservaRepository;
    private final IPagoRepository pagoRepository;
    private final IPropiedadRepository propiedadRepository;
    private final IHabitacionRepository habitacionRepository;

    public ReporteService(IReservaRepository reservaRepository, IPagoRepository pagoRepository,
            IPropiedadRepository propiedadRepository, IHabitacionRepository habitacionRepository) {
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.propiedadRepository = propiedadRepository;
        this.habitacionRepository = habitacionRepository;
    }

    /** Ocupación de todas las propiedades, o de una sola si se indica propiedadId. */
    public List<Ocupacion> ocupacion(LocalDate desde, LocalDate hasta, Long propiedadId) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new SolicitudInvalidaException("El rango debe tener al menos un día: 'hasta' debe ser posterior a 'desde'.");
        }
        List<PropiedadModel> propiedades = propiedadId == null
                ? propiedadRepository.findAll()
                : List.of(propiedadRepository.findById(propiedadId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Propiedad", propiedadId)));
        return propiedades.stream().map(p -> calcularOcupacion(p, desde, hasta)).toList();
    }

    public Ingresos ingresos(LocalDate desde, LocalDate hasta, Long propiedadId) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new SolicitudInvalidaException("'hasta' no puede ser anterior a 'desde'.");
        }
        if (propiedadId != null && !propiedadRepository.existsById(propiedadId)) {
            throw new RecursoNoEncontradoException("Propiedad", propiedadId);
        }
        List<PagoModel> pagos = pagoRepository.findLiquidadosEntre(EstadoPago.PAGADO, desde, hasta, propiedadId);

        BigDecimal total = BigDecimal.ZERO;
        Map<MetodoPago, BigDecimal> porMetodo = new EnumMap<>(MetodoPago.class);
        for (PagoModel pago : pagos) {
            total = total.add(pago.getMonto());
            if (pago.getMetodoPago() != null) {
                porMetodo.merge(pago.getMetodoPago(), pago.getMonto(), BigDecimal::add);
            }
        }
        return new Ingresos(desde, hasta, propiedadId, total, pagos.size(), porMetodo);
    }

    /** Clientes con pagos PENDIENTES ya vencidos, de mayor a menor deuda. */
    public List<Moroso> morosidad() {
        LocalDate hoy = LocalDate.now();
        List<PagoModel> vencidos = pagoRepository.findByEstadoAndFechaVencimientoBefore(EstadoPago.PENDIENTE, hoy);

        Map<Long, List<PagoModel>> porCliente = vencidos.stream().collect(
                Collectors.groupingBy(p -> p.getReserva().getCliente().getId(), LinkedHashMap::new,
                        Collectors.toList()));

        return porCliente.values().stream().map(pagos -> {
            ClienteModel cliente = pagos.get(0).getReserva().getCliente();
            BigDecimal adeudado = pagos.stream().map(PagoModel::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDate masAntiguo = pagos.stream().map(PagoModel::getFechaVencimiento)
                    .min(Comparator.naturalOrder()).orElseThrow();
            return new Moroso(cliente.getId(), cliente.getNombreCompleto(), cliente.getEmail(),
                    cliente.getTelefono(), adeudado, pagos.size(), masAntiguo,
                    ChronoUnit.DAYS.between(masAntiguo, hoy));
        }).sorted(Comparator.comparing(Moroso::montoAdeudado, Comparator.reverseOrder())).toList();
    }

    private Ocupacion calcularOcupacion(PropiedadModel propiedad, LocalDate desde, LocalDate hasta) {
        long noches = ChronoUnit.DAYS.between(desde, hasta);
        long unidades = Math.max(1, habitacionRepository.countByPropiedad_Id(propiedad.getId()));
        List<ReservaModel> reservas = reservaRepository.findPorPropiedadEnRango(propiedad.getId(), desde, hasta,
                ESTADOS_OCUPADOS);

        long nochesReservadas = 0;
        for (ReservaModel reserva : reservas) {
            long nochesEnRango = nochesEnRango(reserva, desde, hasta);
            // Reservar la propiedad completa ocupa todas sus unidades; una habitación ocupa una.
            nochesReservadas += reserva.getHabitacion() == null ? nochesEnRango * unidades : nochesEnRango;
        }
        long nochesTotales = noches * unidades;
        double porcentaje = nochesTotales == 0 ? 0.0
                : Math.round(nochesReservadas * 10000.0 / nochesTotales) / 100.0;
        return new Ocupacion(propiedad.getId(), propiedad.getTitulo(), desde, hasta, unidades, nochesReservadas,
                nochesTotales, porcentaje);
    }

    private long nochesEnRango(ReservaModel reserva, LocalDate desde, LocalDate hasta) {
        LocalDate inicio = reserva.getFechaInicio().isAfter(desde) ? reserva.getFechaInicio() : desde;
        LocalDate fin = reserva.getFechaFin().isBefore(hasta) ? reserva.getFechaFin() : hasta;
        return Math.max(ChronoUnit.DAYS.between(inicio, fin), 0);
    }
}
