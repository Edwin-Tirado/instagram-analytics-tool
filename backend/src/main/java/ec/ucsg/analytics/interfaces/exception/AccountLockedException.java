package ec.ucsg.analytics.interfaces.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.LOCKED)
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String email) {
        super("La cuenta " + email + " está bloqueada por exceso de intentos fallidos. "
              + "Contacte al administrador para desbloquearla.");
    }
}
