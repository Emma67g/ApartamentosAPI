package com.blue.apartamentos.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.blue.apartamentos.models.ImagenPropiedadModel;
import com.blue.apartamentos.services.ImagenPropiedadService;

import jakarta.validation.Valid;

/** Imágenes de una propiedad (subrecurso de /api/propiedades/{propiedadId}). */
@RestController
@RequestMapping("/api/propiedades/{propiedadId}/imagenes")
public class ImagenPropiedadController {

    private final ImagenPropiedadService imagenService;

    public ImagenPropiedadController(ImagenPropiedadService imagenService) {
        this.imagenService = imagenService;
    }

    @GetMapping
    public List<ImagenPropiedadModel> listar(@PathVariable Long propiedadId) {
        return imagenService.listar(propiedadId);
    }

    @PostMapping
    public ResponseEntity<ImagenPropiedadModel> agregar(@PathVariable Long propiedadId,
            @Valid @RequestBody ImagenPropiedadModel request) {
        ImagenPropiedadModel creada = imagenService.agregar(propiedadId, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.getId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    /** Marca la imagen como portada (la portada anterior deja de serlo). */
    @PatchMapping("/{imagenId}/portada")
    public ImagenPropiedadModel marcarComoPortada(@PathVariable Long propiedadId,
            @PathVariable Long imagenId) {
        return imagenService.marcarComoPortada(propiedadId, imagenId);
    }

    @DeleteMapping("/{imagenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long propiedadId, @PathVariable Long imagenId) {
        imagenService.eliminar(propiedadId, imagenId);
    }
}
