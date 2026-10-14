package com.blue.apartamentos.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blue.apartamentos.services.ReporteService;

/** Reportes de solo lectura: ocupación, ingresos y morosidad. */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** GET /api/reportes/ocupacion?desde=2026-12-01&hasta=2026-12-31[&propiedadId=1] — rango [desde, hasta). */
    @GetMapping("/ocupacion")
    public List<ReporteService.Ocupacion> ocupacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long propiedadId) {
        return reporteService.ocupacion(desde, hasta, propiedadId);
    }

    /** GET /api/reportes/ingresos?desde=2026-12-01&hasta=2026-12-31[&propiedadId=1] — ambos extremos incluidos. */
    @GetMapping("/ingresos")
    public ReporteService.Ingresos ingresos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long propiedadId) {
        return reporteService.ingresos(desde, hasta, propiedadId);
    }

    /** GET /api/reportes/morosidad — clientes con pagos vencidos y sin liquidar. */
    @GetMapping("/morosidad")
    public List<ReporteService.Moroso> morosidad() {
        return reporteService.morosidad();
    }
}
