package com.blue.apartamentos.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.blue.apartamentos.models.PropiedadModel;
import com.blue.apartamentos.models.PropiedadModel.EstadoPropiedad;
import com.blue.apartamentos.models.PropiedadModel.TipoPropiedad;
import com.blue.apartamentos.services.PropiedadService;

import jakarta.validation.Valid;

/** Casas y pisos completos en renta. */
@RestController
@RequestMapping("/api/propiedades")
public class PropiedadController {

    private final PropiedadService propiedadService;

    public PropiedadController(PropiedadService propiedadService) {
        this.propiedadService = propiedadService;
    }

    /** GET /api/propiedades?tipo=PISO&propietarioId=1&ciudad=Veracruz&estado=DISPONIBLE (todos opcionales). */
    @GetMapping
    public List<PropiedadModel> listar(@RequestParam(required = false) TipoPropiedad tipo,
            @RequestParam(required = false) Long propietarioId,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) EstadoPropiedad estado) {
        return propiedadService.listar(tipo, propietarioId, ciudad, estado);
    }

    @GetMapping("/{id}")
    public PropiedadModel obtener(@PathVariable Long id) {
        return propiedadService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<PropiedadModel> crear(@Valid @RequestBody PropiedadModel request) {
        PropiedadModel creada = propiedadService.crear(request);
        return ResponseEntity.created(ubicacion(creada.getId())).body(creada);
    }

    @PutMapping("/{id}")
    public PropiedadModel actualizar(@PathVariable Long id, @Valid @RequestBody PropiedadModel request) {
        return propiedadService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        propiedadService.eliminar(id);
    }

    private URI ubicacion(Long id) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    }
}
