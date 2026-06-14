package ec.ucsg.analytics.infrastructure.security;

import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsula la política de bloqueo por intentos fallidos.
 *
 * Regla de negocio: tras MAX_ATTEMPTS consecutivos, la cuenta queda
 * bloqueada hasta que un ADMIN la desbloquee manualmente (no hay
 * desbloqueo automático por tiempo, para mayor seguridad).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;

    @Transactional
    public void loginSucceeded(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0) {
                user.resetFailedAttempts();
                userRepository.save(user);
            }
        });
    }

    @Transactional
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

    @Transactional(readOnly = true)
    public boolean isLocked(String email) {
        return userRepository.findByEmail(email)
            .map(AppUser::isLocked)
            .orElse(false);
    }
}
