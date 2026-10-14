package com.blue.apartamentos.services;

import java.time.LocalDate;

/** Validaciones pequeñas compartidas por los servicios. */
final class Validaciones {

    private Validaciones() {
    }

    static void rangoDeFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new SolicitudInvalidaException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
    }

    /** Las relaciones se envían como objeto con su id (por ejemplo "cliente": {"id": 1}); exige que el id venga. */
    static Long exigirId(Long id, String campo) {
        if (id == null) {
            throw new SolicitudInvalidaException("Debe indicar " + campo + ".");
        }
        return id;
    }

    /** Recorta espacios y convierte el texto vacío en null. */
    static String textoONull(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
