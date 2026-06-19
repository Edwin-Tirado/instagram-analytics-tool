package ec.ucsg.analytics.interfaces.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmailDomainNotAllowedException extends RuntimeException {

    public EmailDomainNotAllowedException(String email) {
        super("El dominio del correo no está permitido: " + email
              + ". Solo se aceptan cuentas @cu.ucsg.edu.ec");
    }
}
