package com.blue.apartamentos.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.blue.apartamentos.services.ConflictoException;
import com.blue.apartamentos.services.RecursoNoEncontradoException;
import com.blue.apartamentos.services.SolicitudInvalidaException;

/**
 * Traduce todas las excepciones a respuestas JSON con el mismo formato (ErrorResponse):
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Cuerpo estándar de todas las respuestas de error de la API. */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String mensaje,
            String ruta,
            List<String> detalles) {
    }

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> handleNoEncontrado(RecursoNoEncontradoException ex, WebRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Object> handleSolicitudInvalida(SolicitudInvalidaException ex, WebRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Object> handleConflicto(ConflictoException ex, WebRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), List.of(), request);
    }

    /** Red de seguridad: restricciones de la base de datos (únicos, llaves foráneas) que no se previnieron antes. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegridad(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Violación de integridad de datos", ex);
        return construir(HttpStatus.CONFLICT,
                "La operación viola una restricción de la base de datos (registro duplicado o con datos relacionados).",
                List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleInesperado(Exception ex, WebRequest request) {
        log.error("Error inesperado", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno en el servidor.", List.of(),
                request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos.", detalles, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud falta o es inválido (JSON mal formado, fecha o valor de enumeración incorrecto).",
                List.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String parametro = ex.getPropertyName() != null ? ex.getPropertyName() : "desconocido";
        return construir(HttpStatus.BAD_REQUEST,
                "El valor '" + ex.getValue() + "' no es válido para el parámetro '" + parametro + "'.", List.of(),
                request);
    }

    /** Cualquier otro error de Spring MVC (405, 415, parámetro faltante, ruta inexistente...) mantiene su código. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        return construir(statusCode, ex.getMessage(), List.of(), request);
    }

    private ResponseEntity<Object> construir(HttpStatusCode status, String mensaje, List<String> detalles,
            WebRequest request) {
        HttpStatus http = HttpStatus.resolve(status.value());
        String razon = http != null ? http.getReasonPhrase() : "Error";
        String ruta = request.getDescription(false).replaceFirst("^uri=", "");
        ErrorResponse cuerpo = new ErrorResponse(LocalDateTime.now(), status.value(), razon, mensaje, ruta,
                detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
