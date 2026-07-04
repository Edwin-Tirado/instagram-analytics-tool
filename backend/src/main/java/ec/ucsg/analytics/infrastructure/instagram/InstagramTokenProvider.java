package ec.ucsg.analytics.infrastructure.instagram;

import ec.ucsg.analytics.domain.model.InstagramToken;
import ec.ucsg.analytics.domain.repository.InstagramTokenRepository;
import ec.ucsg.analytics.infrastructure.instagram.dto.InstagramTokenRefreshResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

/**
 * Fuente de verdad del access token de Instagram — vive en BD (tabla
 * {@code instagram_token}, fila única) en vez de solo en application.yml,
 * para poder renovarlo en caliente sin reiniciar el backend.
 *
 * La primera vez que se pide el token, se "siembra" desde
 * {@code app.instagram.access-token} (fallback de application.yml) y a
 * partir de ahí la BD manda — cada renovación actualiza esa misma fila.
 */
@Slf4j
@Service
public class InstagramTokenProvider {

    /**
     * Endpoint fijo de Meta para renovar tokens de larga duración de
     * "Instagram API with Instagram Login". NO lleva prefijo de versión
     * (a diferencia de /v19.0/{id}/media) — por eso no reutiliza el
     * RestClient versionado de InstagramGraphApiClient.
     */
    private static final String REFRESH_URL = "https://graph.instagram.com/refresh_access_token";
    private static final int    TOKEN_ROW_ID = 1;

    private final InstagramTokenRepository tokenRepository;
    private final RestClient               restClient;
    private final String                   fallbackAccessToken;

    public InstagramTokenProvider(
            InstagramTokenRepository tokenRepository,
            @Value("${app.instagram.access-token}") String fallbackAccessToken) {
        this.tokenRepository     = tokenRepository;
        this.fallbackAccessToken = fallbackAccessToken;
        this.restClient          = RestClient.create();
    }

    /** Token vigente — lo siembra desde application.yml la primera vez que se llama. */
    @Transactional
    public String getCurrentToken() {
        return tokenRepository.findById(TOKEN_ROW_ID)
            .map(InstagramToken::getAccessToken)
            .orElseGet(this::seedFromConfig);
    }

    private String seedFromConfig() {
        tokenRepository.save(
            InstagramToken.builder()
                .id(TOKEN_ROW_ID)
                .accessToken(fallbackAccessToken)
                .updatedAt(LocalDateTime.now())
                .build()
        );
        log.info("Token de Instagram sembrado en BD desde application.yml (primera vez)");
        return fallbackAccessToken;
    }

    /**
     * Renueva el token de larga duración actual. Meta exige que tenga al
     * menos 24h de antigüedad y no haya expirado — no requiere el App
     * Secret (a diferencia del intercambio inicial short-lived → long-lived).
     */
    @Transactional
    public void refreshToken() {
        String current = getCurrentToken();

        String url = UriComponentsBuilder.fromUriString(REFRESH_URL)
            .queryParam("grant_type", "ig_refresh_token")
            .queryParam("access_token", current)
            .toUriString();

        InstagramTokenRefreshResponse response = restClient.get()
            .uri(url)
            .retrieve()
            .body(InstagramTokenRefreshResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Respuesta vacía al renovar el token de Instagram");
        }

        InstagramToken token = tokenRepository.findById(TOKEN_ROW_ID)
            .orElseGet(() -> InstagramToken.builder().id(TOKEN_ROW_ID).build());
        token.setAccessToken(response.accessToken());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(response.expiresIn()));
        token.setUpdatedAt(LocalDateTime.now());
        tokenRepository.save(token);

        log.info("Token de Instagram renovado — válido hasta {}", token.getExpiresAt());
    }
}
