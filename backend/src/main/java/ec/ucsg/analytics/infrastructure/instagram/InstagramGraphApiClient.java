package ec.ucsg.analytics.infrastructure.instagram;

import ec.ucsg.analytics.infrastructure.instagram.dto.InstagramChildrenResponse;
import ec.ucsg.analytics.infrastructure.instagram.dto.InstagramMediaItem;
import ec.ucsg.analytics.infrastructure.instagram.dto.InstagramMediaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cliente HTTP para la Instagram Graph API v19.0.
 *
 * Usa {@link RestClient} (Spring 6.1) — síncrono, sin dependencias extra.
 *
 * Autenticación: Long-Lived User Access Token configurado en la variable
 * de entorno {@code INSTAGRAM_LONG_LIVED_TOKEN}. El token debe pertenecer
 * a un usuario Business/Creator que administre la cuenta @ucsgnotificaciones.
 *
 * Paginación: la API devuelve hasta 25 posts por página con cursor-based
 * paging. Este cliente itera todas las páginas automáticamente.
 */
@Slf4j
@Component
public class InstagramGraphApiClient {

    private static final String MEDIA_FIELDS =
        "id,caption,media_type,media_url,permalink,timestamp";

    private static final String CHILDREN_FIELDS = "id,media_url,media_type";

    /** Máximo de páginas a consumir por ejecución (failsafe anti-bucle). */
    private static final int MAX_PAGES = 20;

    private final RestClient  restClient;
    private final String      accountId;
    private final String      accessToken;
    private final int         daysLookback;

    public InstagramGraphApiClient(
            @Value("${app.instagram.base-url}")    String baseUrl,
            @Value("${app.instagram.account-id}")  String accountId,
            @Value("${app.instagram.access-token}") String accessToken,
            @Value("${app.instagram.days-lookback:30}") int daysLookback) {

        this.accountId   = accountId;
        this.accessToken = accessToken;
        this.daysLookback = daysLookback;

        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    // ── API pública ─────────────────────────────────────────────────

    /**
     * Recupera todos los posts de los últimos {@code daysLookback} días,
     * iterando la paginación cursor-based de la Graph API.
     */
    public List<InstagramMediaItem> fetchRecentPosts() {
        long sinceTimestamp = Instant.now()
            .minus(daysLookback, ChronoUnit.DAYS)
            .getEpochSecond();

        List<InstagramMediaItem> allPosts = new ArrayList<>();
        String nextUrl = buildInitialUrl(sinceTimestamp);
        int page = 0;

        while (nextUrl != null && page < MAX_PAGES) {
            try {
                InstagramMediaResponse response = restClient.get()
                    .uri(nextUrl)
                    .retrieve()
                    .body(InstagramMediaResponse.class);

                if (response == null || response.getData() == null) break;

                allPosts.addAll(response.getData());
                log.debug("Página {} — {} posts obtenidos", ++page, response.getData().size());

                nextUrl = (response.getPaging() != null)
                    ? response.getPaging().getNext()
                    : null;

            } catch (RestClientException e) {
                log.error("Error llamando a la Graph API (página {}): {}", page, e.getMessage());
                break;
            }
        }

        log.info("Ingesta completada: {} posts totales de los últimos {} días", allPosts.size(), daysLookback);
        return allPosts;
    }

    /**
     * Recupera las imágenes individuales de un CAROUSEL_ALBUM.
     * Devuelve lista vacía ante cualquier error de red (fail-safe).
     */
    public List<InstagramMediaItem> fetchChildren(String mediaId) {
        String url = UriComponentsBuilder
            .fromPath("/{mediaId}/children")
            .queryParam("fields",       CHILDREN_FIELDS)
            .queryParam("access_token", accessToken)
            .buildAndExpand(mediaId)
            .toUriString();

        try {
            InstagramChildrenResponse response = restClient.get()
                .uri(url)
                .retrieve()
                .body(InstagramChildrenResponse.class);

            return (response != null && response.getData() != null)
                ? response.getData()
                : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Error obteniendo hijos del carrusel {}: {}", mediaId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Privados ────────────────────────────────────────────────────

    private String buildInitialUrl(long sinceTimestamp) {
        return UriComponentsBuilder
            .fromPath("/{accountId}/media")
            .queryParam("fields",       MEDIA_FIELDS)
            .queryParam("since",        sinceTimestamp)
            .queryParam("access_token", accessToken)
            .buildAndExpand(accountId)
            .toUriString();
    }
}
