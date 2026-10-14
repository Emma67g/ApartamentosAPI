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

import com.blue.apartamentos.models.ClienteModel;
import com.blue.apartamentos.models.ClienteModel.ClienteStatus;
import com.blue.apartamentos.models.ClienteModel.TipoCliente;
import com.blue.apartamentos.services.ClienteService;
import com.blue.apartamentos.services.PagoService;

import jakarta.validation.Valid;

/** Arrendatarios y propietarios. */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final PagoService pagoService;

    public ClienteController(ClienteService clienteService, PagoService pagoService) {
        this.clienteService = clienteService;
        this.pagoService = pagoService;
    }

    /** GET /api/clientes?tipo=PROPIETARIO&estatus=ACTIVO (ambos filtros son opcionales). */
    @GetMapping
    public List<ClienteModel> listar(@RequestParam(required = false) TipoCliente tipo,
            @RequestParam(required = false) ClienteStatus estatus) {
        return clienteService.listar(tipo, estatus);
    }

    @GetMapping("/{id}")
    public ClienteModel obtener(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    /** Facturado, pagado, saldo y monto vencido de las reservas activas del cliente. */
    @GetMapping("/{id}/estado-cuenta")
    public PagoService.EstadoCuenta estadoCuenta(@PathVariable Long id) {
        return pagoService.estadoCuenta(id);
    }

    @PostMapping
    public ResponseEntity<ClienteModel> crear(@Valid @RequestBody ClienteModel request) {
        ClienteModel creado = clienteService.crear(request);
        return ResponseEntity.created(ubicacion(creado.getId())).body(creado);
    }

    @PutMapping("/{id}")
    public ClienteModel actualizar(@PathVariable Long id, @Valid @RequestBody ClienteModel request) {
        return clienteService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
    }

    private URI ubicacion(Long id) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    }
}
