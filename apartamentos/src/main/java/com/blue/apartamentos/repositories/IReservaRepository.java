package com.blue.apartamentos.repositories;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.blue.apartamentos.models.ReservaModel;
import com.blue.apartamentos.models.ReservaModel.EstadoReserva;

@Repository
public interface IReservaRepository extends JpaRepository<ReservaModel, Long> {

    /** Valor para "no excluir ninguna reserva" (los ids generados siempre son mayores a cero). */
    long SIN_EXCLUSION = 0L;

    @Query("""
            SELECT r FROM ReservaModel r LEFT JOIN r.habitacion h
            WHERE (:clienteId IS NULL OR r.cliente.id = :clienteId)
              AND (:propiedadId IS NULL OR r.propiedad.id = :propiedadId)
              AND (:habitacionId IS NULL OR h.id = :habitacionId)
              AND (:estado IS NULL OR r.estado = :estado)
            ORDER BY r.fechaInicio DESC
            """)
    List<ReservaModel> buscar(@Param("clienteId") Long clienteId,
            @Param("propiedadId") Long propiedadId,
            @Param("habitacionId") Long habitacionId,
            @Param("estado") EstadoReserva estado);

    List<ReservaModel> findByCliente_IdAndEstadoNotOrderByFechaInicioDesc(Long clienteId, EstadoReserva estado);

    boolean existsByCliente_Id(Long clienteId);

    boolean existsByPropiedad_Id(Long propiedadId);

    boolean existsByHabitacion_Id(Long habitacionId);

    /** Reservas de la propiedad (completa o por habitación) con los estados dados que tocan [desde, hasta). */
    @Query("""
            SELECT r FROM ReservaModel r
            WHERE r.propiedad.id = :propiedadId AND r.estado IN :estados
              AND r.fechaInicio < :hasta AND r.fechaFin > :desde
            """)
    List<ReservaModel> findPorPropiedadEnRango(@Param("propiedadId") Long propiedadId,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("estados") Collection<EstadoReserva> estados);

    /** Cualquier reserva activa de la propiedad (completa o de alguna habitación) cruza el rango. */
    @Query("""
            SELECT COUNT(r) FROM ReservaModel r
            WHERE r.propiedad.id = :propiedadId AND r.estado <> :estadoExcluido AND r.id <> :excluirId
              AND r.fechaInicio < :fin AND r.fechaFin > :inicio
            """)
    long contarSolapesPropiedad(@Param("propiedadId") Long propiedadId,
            @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin,
            @Param("excluirId") Long excluirId, @Param("estadoExcluido") EstadoReserva estadoExcluido);

    /** Reservas activas de esa habitación, o de la propiedad completa, que cruzan el rango. */
    @Query("""
            SELECT COUNT(r) FROM ReservaModel r LEFT JOIN r.habitacion h
            WHERE r.propiedad.id = :propiedadId AND r.estado <> :estadoExcluido AND r.id <> :excluirId
              AND (h IS NULL OR h.id = :habitacionId)
              AND r.fechaInicio < :fin AND r.fechaFin > :inicio
            """)
    long contarSolapesHabitacion(@Param("propiedadId") Long propiedadId,
            @Param("habitacionId") Long habitacionId,
            @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin,
            @Param("excluirId") Long excluirId, @Param("estadoExcluido") EstadoReserva estadoExcluido);

    default boolean hayReservaEnPropiedad(Long propiedadId, LocalDate inicio, LocalDate fin, Long excluirId) {
        return contarSolapesPropiedad(propiedadId, inicio, fin, excluirId, EstadoReserva.CANCELADA) > 0;
    }

    default boolean hayReservaEnHabitacion(Long propiedadId, Long habitacionId, LocalDate inicio, LocalDate fin,
            Long excluirId) {
        return contarSolapesHabitacion(propiedadId, habitacionId, inicio, fin, excluirId,
                EstadoReserva.CANCELADA) > 0;
    }
}
