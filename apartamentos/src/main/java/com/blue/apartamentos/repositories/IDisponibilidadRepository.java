package com.blue.apartamentos.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.blue.apartamentos.models.DisponibilidadModel;

@Repository
public interface IDisponibilidadRepository extends JpaRepository<DisponibilidadModel, Long> {

    @Query("""
            SELECT d FROM DisponibilidadModel d LEFT JOIN d.habitacion h
            WHERE (:propiedadId IS NULL OR d.propiedad.id = :propiedadId)
              AND (:habitacionId IS NULL OR h.id = :habitacionId)
            ORDER BY d.fechaInicio
            """)
    List<DisponibilidadModel> buscar(@Param("propiedadId") Long propiedadId,
            @Param("habitacionId") Long habitacionId);

    /** Bloqueos que afectan a la propiedad completa: los de la propiedad y los de cualquiera de sus habitaciones. */
    @Query("""
            SELECT COUNT(d) FROM DisponibilidadModel d
            WHERE d.propiedad.id = :propiedadId AND d.disponible = false
              AND d.fechaInicio < :fin AND d.fechaFin > :inicio
            """)
    long contarBloqueosPropiedad(@Param("propiedadId") Long propiedadId,
            @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    /** Bloqueos que afectan a una habitación: los de esa habitación y los de la propiedad completa. */
    @Query("""
            SELECT COUNT(d) FROM DisponibilidadModel d LEFT JOIN d.habitacion h
            WHERE d.propiedad.id = :propiedadId AND d.disponible = false
              AND (h IS NULL OR h.id = :habitacionId)
              AND d.fechaInicio < :fin AND d.fechaFin > :inicio
            """)
    long contarBloqueosHabitacion(@Param("propiedadId") Long propiedadId,
            @Param("habitacionId") Long habitacionId,
            @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    /** Tarifas especiales de la propiedad completa que tocan el rango; la más reciente primero. */
    @Query("""
            SELECT d FROM DisponibilidadModel d
            WHERE d.propiedad.id = :propiedadId AND d.habitacion IS NULL
              AND d.disponible = true AND d.precioEspecial IS NOT NULL
              AND d.fechaInicio < :fin AND d.fechaFin > :inicio
            ORDER BY d.id DESC
            """)
    List<DisponibilidadModel> findTarifasEspecialesPropiedad(@Param("propiedadId") Long propiedadId,
            @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    /** Tarifas especiales de una habitación que tocan el rango; la más reciente primero. */
    @Query("""
            SELECT d FROM DisponibilidadModel d
            WHERE d.habitacion.id = :habitacionId
              AND d.disponible = true AND d.precioEspecial IS NOT NULL
              AND d.fechaInicio < :fin AND d.fechaFin > :inicio
            ORDER BY d.id DESC
            """)
    List<DisponibilidadModel> findTarifasEspecialesHabitacion(@Param("habitacionId") Long habitacionId,
            @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    void deleteByPropiedad_Id(Long propiedadId);

    void deleteByHabitacion_Id(Long habitacionId);
}
