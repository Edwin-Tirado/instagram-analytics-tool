package ec.ucsg.analytics.interfaces.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando un usuario con cuenta bloqueada (locked=true) intenta acceder.
 *
 * HTTP 403 Forbidden — el bloqueo es absoluto e inmediato: el primer intento
 * de acceso con una cuenta bloqueada falla con este error, sin tolerancia de
 * intentos adicionales. El desbloqueo es exclusivamente manual por un ADMIN.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String email) {
        super("Cuenta suspendida, contacte al administrador.");
    }
}
