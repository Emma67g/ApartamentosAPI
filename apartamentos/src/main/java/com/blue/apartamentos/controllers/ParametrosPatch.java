package com.blue.apartamentos.controllers;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import com.blue.apartamentos.services.SolicitudInvalidaException;

/**
 * Lectura de cuerpos PATCH parciales ({"estado": "..."}). Se leen como campos sueltos y no como entidad
 * para que un campo omitido sea realmente "no enviado" y no el valor por defecto de la entidad.
 */
final class ParametrosPatch {

    private ParametrosPatch() {
    }

    /** Devuelve null si el campo no viene; lanza 400 si trae un valor que no pertenece al enum. */
    static <E extends Enum<E>> E enumeracion(Map<String, Object> cuerpo, String campo, Class<E> tipo) {
        String texto = texto(cuerpo, campo);
        if (texto == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, texto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SolicitudInvalidaException("Valor inválido '" + texto + "' para '" + campo
                    + "'. Valores permitidos: " + Arrays.toString(tipo.getEnumConstants()) + ".");
        }
    }

    /** Devuelve null si el campo no viene o está vacío. */
    static String texto(Map<String, Object> cuerpo, String campo) {
        Object valor = cuerpo.get(campo);
        if (valor == null || String.valueOf(valor).isBlank()) {
            return null;
        }
        return String.valueOf(valor).trim();
    }
}
