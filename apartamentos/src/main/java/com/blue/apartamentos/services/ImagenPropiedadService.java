package com.blue.apartamentos.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blue.apartamentos.models.ImagenPropiedadModel;
import com.blue.apartamentos.models.PropiedadModel;
import com.blue.apartamentos.repositories.IImagenPropiedadRepository;
import com.blue.apartamentos.repositories.IPropiedadRepository;

@Service
@Transactional(readOnly = true)
public class ImagenPropiedadService {

    private final IImagenPropiedadRepository imagenRepository;
    private final IPropiedadRepository propiedadRepository;

    public ImagenPropiedadService(IImagenPropiedadRepository imagenRepository,
            IPropiedadRepository propiedadRepository) {
        this.imagenRepository = imagenRepository;
        this.propiedadRepository = propiedadRepository;
    }

    public List<ImagenPropiedadModel> listar(Long propiedadId) {
        buscarPropiedad(propiedadId);
        return imagenRepository.findByPropiedad_IdOrderByOrdenAsc(propiedadId);
    }

    @Transactional
    public ImagenPropiedadModel agregar(Long propiedadId, ImagenPropiedadModel datos) {
        PropiedadModel propiedad = buscarPropiedad(propiedadId);
        // La primera imagen de una propiedad es su portada; después solo si se pide explícitamente.
        boolean portada = !imagenRepository.existsByPropiedad_Id(propiedadId) || datos.isPortada();
        if (portada) {
            quitarPortadaActual(propiedadId);
        }
        ImagenPropiedadModel imagen = new ImagenPropiedadModel();
        imagen.setPropiedad(propiedad);
        imagen.setUrl(datos.getUrl().trim());
        imagen.setOrden(datos.getOrden());
        imagen.setPortada(portada);
        return imagenRepository.save(imagen);
    }

    @Transactional
    public ImagenPropiedadModel marcarComoPortada(Long propiedadId, Long imagenId) {
        ImagenPropiedadModel imagen = buscarImagen(propiedadId, imagenId);
        if (!imagen.isPortada()) {
            quitarPortadaActual(propiedadId);
            imagen.setPortada(true);
            imagenRepository.save(imagen);
        }
        return imagen;
    }

    @Transactional
    public void eliminar(Long propiedadId, Long imagenId) {
        ImagenPropiedadModel imagen = buscarImagen(propiedadId, imagenId);
        boolean eraPortada = imagen.isPortada();
        imagenRepository.delete(imagen);
        imagenRepository.flush();
        if (eraPortada) {
            // Si se borra la portada, la siguiente imagen por orden ocupa su lugar.
            imagenRepository.findFirstByPropiedad_IdOrderByOrdenAsc(propiedadId).ifPresent(siguiente -> {
                siguiente.setPortada(true);
                imagenRepository.save(siguiente);
            });
        }
    }

    private PropiedadModel buscarPropiedad(Long propiedadId) {
        return propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propiedad", propiedadId));
    }

    private ImagenPropiedadModel buscarImagen(Long propiedadId, Long imagenId) {
        return imagenRepository.findByIdAndPropiedad_Id(imagenId, propiedadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Imagen de la propiedad " + propiedadId, imagenId));
    }

    private void quitarPortadaActual(Long propiedadId) {
        imagenRepository.findFirstByPropiedad_IdAndPortadaTrue(propiedadId).ifPresent(actual -> {
            actual.setPortada(false);
            imagenRepository.save(actual);
        });
    }
}
