package ec.ucsg.analytics.application.service;

import ec.ucsg.analytics.application.dto.request.LoginRequest;
import ec.ucsg.analytics.application.dto.request.RegisterRequest;
import ec.ucsg.analytics.application.dto.response.AuthResponse;
import ec.ucsg.analytics.domain.enums.RoleName;
import ec.ucsg.analytics.domain.model.AppRole;
import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.repository.RoleRepository;
import ec.ucsg.analytics.domain.repository.UserRepository;
import ec.ucsg.analytics.infrastructure.security.JwtService;
import ec.ucsg.analytics.infrastructure.security.LoginAttemptService;
import ec.ucsg.analytics.interfaces.exception.AccountLockedException;
import ec.ucsg.analytics.interfaces.exception.EmailDomainNotAllowedException;
import ec.ucsg.analytics.interfaces.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Casos de uso de autenticación.
 *
 * Validación de dominio — defensa en profundidad (dos capas):
 *   Capa 1 → @Pattern en RegisterRequest (rechaza antes de llegar aquí).
 *   Capa 2 → validación programática en register() (por si se invoca el
 *             servicio desde otro punto de entrada que no sea el controller).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ALLOWED_DOMAIN = "@cu.ucsg.edu.ec";

    private final UserRepository      userRepository;
    private final RoleRepository      roleRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtService          jwtService;
    private final LoginAttemptService loginAttemptService;
    private final AuthenticationManager authenticationManager;

    // ── Registro ────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // 1. Validar dominio estrictamente (capa 2)
        if (!request.email().toLowerCase().endsWith(ALLOWED_DOMAIN)) {
            throw new EmailDomainNotAllowedException(request.email());
        }

        // 2. Verificar que el email no esté ya registrado
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new UserAlreadyExistsException(request.email());
        }

        // 3. Resolver el rol por defecto
        AppRole defaultRole = roleRepository.findByName(RoleName.ROLE_USER)
            .orElseThrow(() -> new IllegalStateException(
                "El rol ROLE_USER no existe en la base de datos. Verifique los datos semilla de V1."
            ));

        // 4. Persistir el nuevo usuario
        AppUser newUser = AppUser.builder()
            .email(request.email().toLowerCase())
            .password(passwordEncoder.encode(request.password()))
            .fullName(request.fullName().trim())
            .roles(Set.of(defaultRole))
            .enabled(true)
            .build();

        AppUser savedUser = userRepository.save(newUser);
        log.info("Nuevo usuario registrado: {}", savedUser.getEmail());

        // 5. Generar tokens y construir respuesta
        return buildAuthResponse(savedUser);
    }

    // ── Login ────────────────────────────────────────────────────────

    // isLocked() usa REQUIRES_NEW (sesion Hibernate propia y limpia), por lo que
    // siempre lee el estado real del DB sin importar el cache de la sesion padre.
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();

        // 1. Verificar estado de bloqueo ANTES de llamar al AuthenticationManager.
        //    Si la cuenta está bloqueada, Spring lanzaría LockedException, pero
        //    devolvemos nuestro mensaje personalizado.
        if (loginAttemptService.isLocked(email)) {
            throw new AccountLockedException(email);
        }

        // 2. Delegar autenticacion a Spring Security
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (LockedException e) {
            // Spring Security detecto via UserDetails.isAccountNonLocked() que la cuenta
            // esta bloqueada (bloqueo manual por admin). Devolvemos 423 directamente,
            // sin registrar intento fallido adicional.
            log.warn("Login rechazado — cuenta bloqueada (LockedException) para: {}", email);
            throw new AccountLockedException(email);
        } catch (BadCredentialsException e) {
            // Registrar el intento fallido (puede desencadenar bloqueo)
            loginAttemptService.loginFailed(email);

            // Verificamos si ahora quedo bloqueado para dar mensaje especifico
            if (loginAttemptService.isLocked(email)) {
                log.warn("Cuenta bloqueada tras multiples intentos: {}", email);
                throw new AccountLockedException(email);
            }

            // Mensaje generico — no revelar si es email o password incorrecto
            throw new BadCredentialsException("Credenciales invalidas");
        }

        // 3. Autenticación exitosa — resetear contador de intentos
        loginAttemptService.loginSucceeded(email);

        AppUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado: " + email));

        log.info("Login exitoso: {}", email);
        return buildAuthResponse(user);
    }

    // ── Perfil del usuario autenticado ──────────────────────────────

    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getMe(String email) {
        AppUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Usuario no encontrado: " + email));
        Set<String> roleNames = user.getRoles().stream()
            .map(r -> r.getName().name())
            .collect(Collectors.toSet());
        return new AuthResponse.UserInfo(
            user.getId().toString(),
            user.getEmail(),
            user.getFullName(),
            roleNames
        );
    }

    // ── Refresh Token ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);

        AppUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Token de refresco inválido"));

        if (!jwtService.isRefreshTokenValid(refreshToken, email)) {
            throw new BadCredentialsException("Token de refresco expirado o inválido");
        }

        if (user.isLocked() || !user.isEnabled()) {
            throw new AccountLockedException(email);
        }

        return buildAuthResponse(user);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(AppUser user) {
        Set<String> roleNames = user.getRoles().stream()
            .map(r -> r.getName().name())
            .collect(Collectors.toSet());

        // Construir UserDetails mínimo para la firma del token
        org.springframework.security.core.userdetails.User principal =
            new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream()
                    .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r.getName().name()))
                    .collect(Collectors.toSet())
            );

        String accessToken  = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        long   expiresIn    = jwtService.getAccessExpirationMs() / 1000;

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
            user.getId().toString(),
            user.getEmail(),
            user.getFullName(),
            roleNames
        );

        return AuthResponse.of(accessToken, refreshToken, expiresIn, userInfo);
    }
}
