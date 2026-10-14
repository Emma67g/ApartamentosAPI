package com.blue.apartamentos.controllers;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.blue.apartamentos.models.HabitacionModel;
import com.blue.apartamentos.models.HabitacionModel.EstadoHabitacion;
import com.blue.apartamentos.models.HabitacionModel.TipoHabitacion;
import com.blue.apartamentos.services.HabitacionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/habitaciones")
public class HabitacionController {

    private final HabitacionService habitacionService;

    public HabitacionController(HabitacionService habitacionService) {
        this.habitacionService = habitacionService;
    }

    /** GET /api/habitaciones?propiedadId=1&estado=DISPONIBLE&tipo=SUITE (todos opcionales). */
    @GetMapping
    public List<HabitacionModel> listar(@RequestParam(required = false) Long propiedadId,
            @RequestParam(required = false) EstadoHabitacion estado,
            @RequestParam(required = false) TipoHabitacion tipo) {
        return habitacionService.listar(propiedadId, estado, tipo);
    }

    @GetMapping("/{id}")
    public HabitacionModel obtener(@PathVariable Long id) {
        return habitacionService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<HabitacionModel> crear(@Valid @RequestBody HabitacionModel request) {
        HabitacionModel creada = habitacionService.crear(request);
        return ResponseEntity.created(ubicacion(creada.getId())).body(creada);
    }

    @PutMapping("/{id}")
    public HabitacionModel actualizar(@PathVariable Long id, @Valid @RequestBody HabitacionModel request) {
        return habitacionService.actualizar(id, request);
    }

    /** PATCH parcial: basta enviar {"estado": "MANTENIMIENTO"}. */
    @PatchMapping("/{id}/estado")
    public HabitacionModel cambiarEstado(@PathVariable Long id,
            @RequestBody Map<String, Object> cuerpo) {
        return habitacionService.cambiarEstado(id,
                ParametrosPatch.enumeracion(cuerpo, "estado", EstadoHabitacion.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        habitacionService.eliminar(id);
    }

    private URI ubicacion(Long id) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    }
}
