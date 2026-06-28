package ec.ucsg.analytics.infrastructure.config;

import ec.ucsg.analytics.domain.enums.RoleName;
import ec.ucsg.analytics.domain.model.AppRole;
import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.repository.RoleRepository;
import ec.ucsg.analytics.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Crea usuarios admin y supervisor al arrancar si no existen.
 * Solo activo en perfil default/dev — nunca en producción.
 *
 * Admin:      admin@cu.ucsg.edu.ec      / Admin@ucsg2026
 * Supervisor: supervisor@cu.ucsg.edu.ec / Super@ucsg2026
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        createIfAbsent("admin@cu.ucsg.edu.ec",      "Admin@ucsg2026",  "Administrador UCSG", RoleName.ROLE_ADMIN);
        createIfAbsent("supervisor@cu.ucsg.edu.ec", "Super@ucsg2026", "Supervisor UCSG",    RoleName.ROLE_SUPERVISOR);
    }

    private void createIfAbsent(String email, String rawPassword, String fullName, RoleName roleName) {
        if (userRepository.existsByEmail(email)) {
            // Actualiza el hash por si el anterior era inválido (migración V5 con hash incorrecto)
            userRepository.findByEmail(email).ifPresent(user -> {
                if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    userRepository.save(user);
                    log.info("DataInitializer: contraseña corregida para {}", email);
                }
            });
            return;
        }

        AppRole role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException("Rol no encontrado: " + roleName));

        AppUser user = AppUser.builder()
            .email(email)
            .password(passwordEncoder.encode(rawPassword))
            .fullName(fullName)
            .roles(Set.of(role))
            .enabled(true)
            .build();

        userRepository.save(user);
        log.info("DataInitializer: usuario creado — {} ({})", email, roleName);
    }
}
