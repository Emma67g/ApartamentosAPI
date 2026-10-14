package com.blue.apartamentos.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.blue.apartamentos.models.PagoModel;
import com.blue.apartamentos.models.PagoModel.EstadoPago;

@Repository
public interface IPagoRepository extends JpaRepository<PagoModel, Long> {

    @Query("""
            SELECT p FROM PagoModel p
            WHERE (:reservaId IS NULL OR p.reserva.id = :reservaId)
              AND (:clienteId IS NULL OR p.reserva.cliente.id = :clienteId)
              AND (:estado IS NULL OR p.estado = :estado)
            ORDER BY p.fechaVencimiento, p.id
            """)
    List<PagoModel> buscar(@Param("reservaId") Long reservaId,
            @Param("clienteId") Long clienteId,
            @Param("estado") EstadoPago estado);

    List<PagoModel> findByReserva_Id(Long reservaId);

    List<PagoModel> findByReserva_IdAndEstado(Long reservaId, EstadoPago estado);

    List<PagoModel> findByReserva_Cliente_Id(Long clienteId);

    boolean existsByReserva_Id(Long reservaId);

    /** Pagos en el estado dado cuyo vencimiento es anterior a la fecha (con PENDIENTE = pagos vencidos). */
    List<PagoModel> findByEstadoAndFechaVencimientoBefore(EstadoPago estado, LocalDate fecha);

    /** Pagos liquidados dentro del rango (ambos extremos incluidos), opcionalmente de una sola propiedad. */
    @Query("""
            SELECT p FROM PagoModel p
            WHERE p.estado = :estado
              AND p.fechaPago BETWEEN :desde AND :hasta
              AND (:propiedadId IS NULL OR p.reserva.propiedad.id = :propiedadId)
            ORDER BY p.fechaPago
            """)
    List<PagoModel> findLiquidadosEntre(@Param("estado") EstadoPago estado,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("propiedadId") Long propiedadId);
}
