package com.blue.apartamentos.services;

/** Los datos enviados incumplen una regla de negocio que no cubren las anotaciones de validación. Se traduce a HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
