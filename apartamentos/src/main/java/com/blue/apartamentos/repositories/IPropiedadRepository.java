package com.blue.apartamentos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.blue.apartamentos.models.PropiedadModel;
import com.blue.apartamentos.models.PropiedadModel.EstadoPropiedad;
import com.blue.apartamentos.models.PropiedadModel.TipoPropiedad;

@Repository
public interface IPropiedadRepository extends JpaRepository<PropiedadModel, Long> {

    @Query("""
            SELECT p FROM PropiedadModel p
            WHERE (:tipo IS NULL OR p.tipo = :tipo)
              AND (:propietarioId IS NULL OR p.propietario.id = :propietarioId)
              AND (:ciudad IS NULL OR LOWER(p.ciudad) = LOWER(:ciudad))
              AND (:estado IS NULL OR p.estado = :estado)
            ORDER BY p.id
            """)
    List<PropiedadModel> buscar(@Param("tipo") TipoPropiedad tipo,
            @Param("propietarioId") Long propietarioId,
            @Param("ciudad") String ciudad,
            @Param("estado") EstadoPropiedad estado);

    boolean existsByPropietario_Id(Long propietarioId);
}
