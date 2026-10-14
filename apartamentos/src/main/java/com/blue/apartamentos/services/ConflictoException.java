package com.blue.apartamentos.services;

/**
 * La petición es válida pero choca con el estado actual del sistema (duplicados, fechas ocupadas,
 * transición de estado no permitida, registros relacionados). Se traduce a HTTP 409.
 */
public class ConflictoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
