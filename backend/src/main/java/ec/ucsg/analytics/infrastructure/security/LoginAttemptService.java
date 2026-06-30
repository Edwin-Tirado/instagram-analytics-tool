package ec.ucsg.analytics.infrastructure.security;

import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsula la politica de bloqueo por intentos fallidos.
 *
 * Regla de negocio: tras MAX_ATTEMPTS consecutivos, la cuenta queda
 * bloqueada hasta que un ADMIN la desbloquee manualmente (no hay
 * desbloqueo automatico por tiempo, para mayor seguridad).
 *
 * IMPORTANTE - Propagation.REQUIRES_NEW en loginFailed / loginSucceeded:
 *   AuthService.login() es @Transactional. Cuando las credenciales son
 *   incorrectas, lanza BadCredentialsException, lo que provoca el rollback
 *   de la transaccion padre. Si loginFailed() participara en esa misma
 *   transaccion, el cambio locked=true tambien haria rollback y NUNCA
 *   se persistiria en base de datos. REQUIRES_NEW abre una transaccion
 *   propia que se confirma antes de que el padre haga rollback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginSucceeded(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0) {
                user.resetFailedAttempts();
                userRepository.save(user);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginFailed(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.incrementFailedAttempts();

            if (user.getFailedLoginAttempts() >= MAX_ATTEMPTS) {
                user.lock();
                log.warn("Cuenta bloqueada por {} intentos fallidos: {}", MAX_ATTEMPTS, email);
            }

            userRepository.save(user);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean isLocked(String email) {
        return userRepository.findByEmail(email)
            .map(AppUser::isLocked)
            .orElse(false);
    }
}
