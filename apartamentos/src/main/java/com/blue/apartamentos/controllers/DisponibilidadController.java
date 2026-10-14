package com.blue.apartamentos.controllers;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.blue.apartamentos.models.DisponibilidadModel;
import com.blue.apartamentos.services.DisponibilidadService;

import jakarta.validation.Valid;

/** Calendario de disponibilidad: bloqueos, tarifas especiales y consulta de fechas libres. */
@RestController
@RequestMapping("/api/disponibilidades")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    /** GET /api/disponibilidades?propiedadId=1&habitacionId=3 (ambos opcionales). */
    @GetMapping
    public List<DisponibilidadModel> listar(@RequestParam(required = false) Long propiedadId,
            @RequestParam(required = false) Long habitacionId) {
        return disponibilidadService.listar(propiedadId, habitacionId);
    }

    /**
     * ¿Está libre la unidad en el rango? 
     * (agregar habitacionId para consultar una habitación en lugar de la propiedad completa).
     */
    @GetMapping("/consulta")
    public DisponibilidadService.Consulta consultar(@RequestParam Long propiedadId,
            @RequestParam(required = false) Long habitacionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return disponibilidadService.consultar(propiedadId, habitacionId, inicio, fin);
    }

    @GetMapping("/{id}")
    public DisponibilidadModel obtener(@PathVariable Long id) {
        return disponibilidadService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<DisponibilidadModel> crear(@Valid @RequestBody DisponibilidadModel request) {
        DisponibilidadModel creada = disponibilidadService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.getId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        disponibilidadService.eliminar(id);
    }
}
