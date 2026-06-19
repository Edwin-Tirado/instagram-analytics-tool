package ec.ucsg.analytics.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Estructura de error uniforme devuelta por GlobalExceptionHandler.
 *
 * El campo {@code errors} solo se serializa cuando contiene elementos
 * (errores de validación campo a campo).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    int         status,
    String      error,
    String      message,
    String      path,
    Instant     timestamp,
    List<FieldError> errors
) {

    public record FieldError(String field, String message) {}

    /** Fábrica para errores simples (sin detalle de campos). */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now(), List.of());
    }

    /** Fábrica para errores de validación con detalle por campo. */
    public static ErrorResponse validation(String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(
            400,
            "Validation Failed",
            "La solicitud contiene datos inválidos",
            path,
            Instant.now(),
            fieldErrors
        );
    }
}
