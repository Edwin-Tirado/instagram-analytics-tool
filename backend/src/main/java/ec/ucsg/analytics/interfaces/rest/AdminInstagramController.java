package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.application.dto.request.UpdateInstagramTokenRequest;
import ec.ucsg.analytics.application.dto.response.InstagramTokenStatusResponse;
import ec.ucsg.analytics.domain.model.InstagramToken;
import ec.ucsg.analytics.infrastructure.instagram.InstagramTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Permite a un ADMIN ver el estado (enmascarado) y actualizar manualmente el
 * access-token de Instagram desde el panel, sin tocar application.yml ni
 * variables de entorno — pensado para cuentas de prueba / entornos locales
 * donde regenerar el token seguido en el dashboard de Meta es más cómodo
 * que redeployar.
 *
 * Rutas:
 *   GET /api/admin/instagram/token → estado actual (enmascarado)
 *   PUT /api/admin/instagram/token → reemplaza el token vigente
 */
@RestController
@RequestMapping("/api/admin/instagram/token")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminInstagramController {

    private final InstagramTokenProvider tokenProvider;

    @GetMapping
    public ResponseEntity<InstagramTokenStatusResponse> getStatus() {
        InstagramToken token = tokenProvider.getStatus();
        if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            return ResponseEntity.ok(new InstagramTokenStatusResponse(false, null, null, null));
        }
        return ResponseEntity.ok(new InstagramTokenStatusResponse(
            true,
            mask(token.getAccessToken()),
            token.getUpdatedAt(),
            token.getExpiresAt()
        ));
    }

    @PutMapping
    public ResponseEntity<InstagramTokenStatusResponse> updateToken(
            @Valid @RequestBody UpdateInstagramTokenRequest request) {
        tokenProvider.updateToken(request.accessToken());
        return getStatus();
    }

    private String mask(String token) {
        int visible = Math.min(4, token.length());
        return "•".repeat(Math.max(0, token.length() - visible)) + token.substring(token.length() - visible);
    }
}
