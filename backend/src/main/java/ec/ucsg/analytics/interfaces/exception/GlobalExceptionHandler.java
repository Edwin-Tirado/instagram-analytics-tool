package ec.ucsg.analytics.interfaces.exception;

import ec.ucsg.analytics.application.dto.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Manejador global de excepciones.
 *
 * Contrato de respuesta uniforme: siempre devuelve {@link ErrorResponse}.
 *
 * Política de mensajes:
 *   - Errores de cliente (4xx): mensaje descriptivo visible al usuario.
 *   - Errores de servidor (5xx): mensaje genérico — el detalle va solo al log.
 *
 * Política de logging:
 *   - 4xx de negocio (dominio, duplicado, bloqueado): nivel WARN.
 *   - 4xx de credenciales: nivel WARN sin exponer datos sensibles.
 *   - 5xx inesperados: nivel ERROR con stack trace completo.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validación de DTOs (@Valid) ──────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldError)
            .toList();

        log.warn("Validación fallida en {}: {} errores", request.getRequestURI(), fieldErrors.size());

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.validation(request.getRequestURI(), fieldErrors));
    }

    // ── Dominio de email no permitido ────────────────────────────────

    @ExceptionHandler(EmailDomainNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleEmailDomain(
            EmailDomainNotAllowedException ex,
            HttpServletRequest request) {

        log.warn("Intento de registro con dominio no permitido en {}", request.getRequestURI());

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(400, "Email Domain Not Allowed", ex.getMessage(), request.getRequestURI()));
    }

    // ── Email ya registrado ──────────────────────────────────────────

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn("Intento de registro duplicado en {}", request.getRequestURI());

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), request.getRequestURI()));
    }

    // ── Cuenta bloqueada ─────────────────────────────────────────────

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request) {

        log.warn("Acceso denegado — cuenta bloqueada en {}", request.getRequestURI());

        return ResponseEntity
            .status(HttpStatus.LOCKED)
            .body(ErrorResponse.of(423, "Account Locked", ex.getMessage(), request.getRequestURI()));
    }

    // ── Credenciales inválidas ───────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        // No logueamos el mensaje original para no filtrar información
        log.warn("Intento de autenticación fallido en {}", request.getRequestURI());

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.of(
                401,
                "Unauthorized",
                "Credenciales inválidas",
                request.getRequestURI()
            ));
    }

    // ── Usuario no encontrado (enmascarado por hideUserNotFoundExceptions=true) ──

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Usuario no encontrado al procesar {}", request.getRequestURI());

        // Mismo mensaje que BadCredentials — no revelar si el email existe
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.of(401, "Unauthorized", "Credenciales inválidas", request.getRequestURI()));
    }

    // ── Cuenta deshabilitada ─────────────────────────────────────────

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            DisabledException ex,
            HttpServletRequest request) {

        log.warn("Intento de acceso con cuenta deshabilitada en {}", request.getRequestURI());

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse.of(
                403,
                "Account Disabled",
                "Esta cuenta está deshabilitada. Contacte al administrador.",
                request.getRequestURI()
            ));
    }

    // ── JWT expirado ─────────────────────────────────────────────────

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            ExpiredJwtException ex,
            HttpServletRequest request) {

        log.debug("JWT expirado en {}", request.getRequestURI());

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.of(401, "Token Expired", "El token ha expirado. Renuévelo.", request.getRequestURI()));
    }

    // ── JWT inválido (firma, formato, etc.) ──────────────────────────

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwt(
            JwtException ex,
            HttpServletRequest request) {

        log.warn("JWT inválido en {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.of(401, "Invalid Token", "El token proporcionado no es válido.", request.getRequestURI()));
    }

    // ── Entidad no encontrada ────────────────────────────────────────

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            jakarta.persistence.EntityNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    // ── Transición de estado inválida ────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request) {

        log.warn("Transición de estado inválida en {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of(409, "Invalid State Transition", ex.getMessage(), request.getRequestURI()));
    }

    // ── Catch-all: errores internos no previstos ─────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error("Error inesperado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
            .internalServerError()
            .body(ErrorResponse.of(
                500,
                "Internal Server Error",
                "Ocurrió un error interno. Por favor, inténtelo más tarde.",
                request.getRequestURI()
            ));
    }

    // ── Utilitarios ──────────────────────────────────────────────────

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
    }
}
