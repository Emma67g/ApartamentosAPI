package com.blue.apartamentos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.blue.apartamentos.models.HabitacionModel;
import com.blue.apartamentos.models.HabitacionModel.EstadoHabitacion;
import com.blue.apartamentos.models.HabitacionModel.TipoHabitacion;

@Repository
public interface IHabitacionRepository extends JpaRepository<HabitacionModel, Long> {

    @Query("""
            SELECT h FROM HabitacionModel h
            WHERE (:propiedadId IS NULL OR h.propiedad.id = :propiedadId)
              AND (:estado IS NULL OR h.estado = :estado)
              AND (:tipo IS NULL OR h.tipo = :tipo)
            ORDER BY h.propiedad.id, h.numero
            """)
    List<HabitacionModel> buscar(@Param("propiedadId") Long propiedadId,
            @Param("estado") EstadoHabitacion estado,
            @Param("tipo") TipoHabitacion tipo);

    boolean existsByPropiedad_Id(Long propiedadId);

    long countByPropiedad_Id(Long propiedadId);

    boolean existsByPropiedad_IdAndNumero(Long propiedadId, String numero);

    boolean existsByPropiedad_IdAndNumeroAndIdNot(Long propiedadId, String numero, Long id);
}
