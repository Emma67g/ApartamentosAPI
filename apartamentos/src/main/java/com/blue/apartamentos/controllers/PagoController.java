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

import com.blue.apartamentos.models.PagoModel;
import com.blue.apartamentos.models.PagoModel.EstadoPago;
import com.blue.apartamentos.models.PagoModel.MetodoPago;
import com.blue.apartamentos.services.PagoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /** GET /api/pagos?reservaId=1&clienteId=2&estado=PENDIENTE&vencidos=true (todos opcionales). */
    @GetMapping
    public List<PagoModel> listar(@RequestParam(required = false) Long reservaId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(defaultValue = "false") boolean vencidos) {
        return pagoService.listar(reservaId, clienteId, estado, vencidos);
    }

    @GetMapping("/{id}")
    public PagoModel obtener(@PathVariable Long id) {
        return pagoService.obtener(id);
    }

    /** Programa un cargo (cuota) PENDIENTE de una reserva. */
    @PostMapping
    public ResponseEntity<PagoModel> crear(@Valid @RequestBody PagoModel request) {
        PagoModel creado = pagoService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.getId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    /** Edita un cargo que sigue PENDIENTE. */
    @PutMapping("/{id}")
    public PagoModel actualizar(@PathVariable Long id, @Valid @RequestBody PagoModel request) {
        return pagoService.actualizar(id, request);
    }

    /** Liquida (PAGADO, indicando método de pago) o cancela un cargo. Cuerpo: estado, metodoPago y referencia. */
    @PatchMapping("/{id}/estado")
    public PagoModel cambiarEstado(@PathVariable Long id, @RequestBody Map<String, Object> cuerpo) {
        return pagoService.cambiarEstado(id,
                ParametrosPatch.enumeracion(cuerpo, "estado", EstadoPago.class),
                ParametrosPatch.enumeracion(cuerpo, "metodoPago", MetodoPago.class),
                ParametrosPatch.texto(cuerpo, "referencia"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        pagoService.eliminar(id);
    }
}
