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

import com.blue.apartamentos.models.ReservaModel;
import com.blue.apartamentos.models.ReservaModel.EstadoReserva;
import com.blue.apartamentos.services.ReservaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /** GET /api/reservas?clienteId=1&propiedadId=2&habitacionId=3&estado=CONFIRMADA (todos opcionales). */
    @GetMapping
    public List<ReservaModel> listar(@RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long propiedadId,
            @RequestParam(required = false) Long habitacionId,
            @RequestParam(required = false) EstadoReserva estado) {
        return reservaService.listar(clienteId, propiedadId, habitacionId, estado);
    }

    @GetMapping("/{id}")
    public ReservaModel obtener(@PathVariable Long id) {
        return reservaService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ReservaModel> crear(@Valid @RequestBody ReservaModel request) {
        ReservaModel creada = reservaService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.getId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    /** Cambia fechas, huéspedes o notas (revalida disponibilidad y recalcula el monto). */
    @PutMapping("/{id}")
    public ReservaModel actualizar(@PathVariable Long id,
            @Valid @RequestBody ReservaModel request) {
        return reservaService.actualizar(id, request);
    }

    /** Confirmar, completar o cancelar una reserva (una reserva cancelada también cancela sus pagos pendientes). */
    @PatchMapping("/{id}/estado")
    public ReservaModel cambiarEstado(@PathVariable Long id,
            @RequestBody Map<String, Object> cuerpo) {
        return reservaService.cambiarEstado(id, ParametrosPatch.enumeracion(cuerpo, "estado", EstadoReserva.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        reservaService.eliminar(id);
    }
}
