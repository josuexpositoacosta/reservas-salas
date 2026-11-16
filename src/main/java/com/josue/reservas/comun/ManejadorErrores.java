package com.josue.reservas.comun;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Todas las respuestas de error usan ProblemDetail (RFC 9457).
 * Los 400 de validación añaden "errores": [{campo, mensaje}] para que el cliente sepa qué corregir.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("campo", e.getField(), "mensaje", String.valueOf(e.getDefaultMessage())))
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos inválidos", "La petición tiene campos con errores");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Cuerpo no legible",
                "El JSON está mal formado o algún valor tiene un tipo incorrecto "
                        + "(las fechas van en formato ISO-8601, p. ej. 2026-12-01T10:00:00)");
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Parámetro inválido",
                "El parámetro '" + ex.getName() + "' tiene un valor no válido: " + ex.getValue());
    }

    @ExceptionHandler(DatosInvalidosException.class)
    ProblemDetail datosInvalidos(DatosInvalidosException ex) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos inválidos", "La petición tiene campos con errores");
        problema.setProperty("errores", List.of(Map.of("campo", ex.getCampo(), "mensaje", ex.getMessage())));
        return problema;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException ex) {
        return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
