package com.blue.apartamentos.services;

/** El recurso solicitado no existe. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " con id " + id + " no existe.");
    }
}
