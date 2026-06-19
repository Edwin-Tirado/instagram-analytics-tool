package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.application.dto.request.LoginRequest;
import ec.ucsg.analytics.application.dto.request.RegisterRequest;
import ec.ucsg.analytics.application.dto.response.AuthResponse;
import ec.ucsg.analytics.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación.
 *
 * Todos protegidos por RateLimitingFilter (10 req/min por IP).
 * No requieren JWT — están excluidos en SecurityConfig y JwtAuthenticationFilter.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registra un nuevo usuario.
     *
     * Restricciones activas:
     *   - @Pattern en RegisterRequest → solo @cu.ucsg.edu.ec
     *   - Validación programática en AuthService (defensa en profundidad)
     *   - Contraseña: mín. 8 chars, mayúscula + minúscula + número + símbolo
     *
     * Respuesta 201 con tokens JWT listos para usar.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Autentica un usuario existente.
     *
     * Comportamiento ante fallos:
     *   - Credenciales incorrectas → 401 (mensaje genérico, sin revelar cuál campo falla)
     *   - Cuenta bloqueada (≥5 intentos) → 423 con instrucción de contactar al admin
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Genera un nuevo access token a partir de un refresh token válido.
     * El refresh token se envía en el header Authorization: Bearer <refreshToken>.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("Authorization") String authorizationHeader) {

        String refreshToken = authorizationHeader.substring("Bearer ".length());
        AuthResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(response);
    }
}
