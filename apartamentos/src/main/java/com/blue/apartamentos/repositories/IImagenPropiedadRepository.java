package com.blue.apartamentos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.blue.apartamentos.models.ImagenPropiedadModel;

@Repository
public interface IImagenPropiedadRepository extends JpaRepository<ImagenPropiedadModel, Long> {

    List<ImagenPropiedadModel> findByPropiedad_IdOrderByOrdenAsc(Long propiedadId);

    Optional<ImagenPropiedadModel> findByIdAndPropiedad_Id(Long id, Long propiedadId);

    Optional<ImagenPropiedadModel> findFirstByPropiedad_IdAndPortadaTrue(Long propiedadId);

    Optional<ImagenPropiedadModel> findFirstByPropiedad_IdOrderByOrdenAsc(Long propiedadId);

    boolean existsByPropiedad_Id(Long propiedadId);

    void deleteByPropiedad_Id(Long propiedadId);
}
