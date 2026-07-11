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
 * Cliente HTTP para "Instagram API with Instagram Login"
 * ({@code base-url: https://graph.instagram.com/v19.0} en application.yml).
 *
 * Usa {@link RestClient} (Spring 6.1) — síncrono, sin dependencias extra.
 *
 * Autenticación: access token de Instagram (prefijo "IGA..."), generado
 * agregando la cuenta como "Instagram tester" en la app de Meta (sección
 * Instagram → "Configuración de la API con inicio de sesión para empresas
 * de Instagram" → "Generar identificadores de acceso"). El token vigente se
 * obtiene en cada llamada vía {@link InstagramTokenProvider} (BD, no
 * application.yml directo) para que la renovación automática semanal
 * ({@code InstagramTokenRefreshJob}) tenga efecto sin reiniciar el backend.
 * NO requiere que @ucsgnotificaciones esté vinculada a ninguna Página de
 * Facebook (a diferencia de la Facebook Graph API clásica vía
 * {@code graph.facebook.com}, que sí lo exige).
 *
 * OJO: NO usar aquí un token "EAA..." de la Facebook Graph API — es un
 * producto distinto con otro formato de token; mezclar ambos hace que la
 * ingesta falle en silencio si el error no se propaga (ver
 * {@link InstagramApiException}).
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

    private final RestClient             restClient;
    private final InstagramTokenProvider tokenProvider;
    private final String                 accountId;
    private final int                    daysLookback;

    public InstagramGraphApiClient(
            @Value("${app.instagram.base-url}")    String baseUrl,
            @Value("${app.instagram.account-id}")  String accountId,
            @Value("${app.instagram.days-lookback:30}") int daysLookback,
            InstagramTokenProvider tokenProvider) {

        this.accountId     = accountId;
        this.daysLookback  = daysLookback;
        this.tokenProvider = tokenProvider;

        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    // ── API pública ─────────────────────────────────────────────────

    /**
     * Recupera todos los posts de los últimos {@code daysLookback} días,
     * iterando la paginación cursor-based de la Graph API.
     *
     * IMPORTANTE: si la API falla (token inválido/expirado, cuenta desvinculada,
     * etc.) esto SE RELANZA como {@link InstagramApiException} — antes se
     * atrapaba aquí y se devolvía la lista parcial en silencio, lo que hacía que
     * un fallo de autenticación se viera idéntico a "no hay posts nuevos"
     * (IngestionRun quedaba en SUCCESS con 0 creados en vez de FAILED con el
     * error real). Quien llama debe decidir qué hacer con el fallo.
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
                throw new InstagramApiException(
                    "Error llamando a la Instagram Graph API: " + e.getMessage(), e);
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
            .queryParam("access_token", tokenProvider.getCurrentToken())
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

    /**
     * Recupera los datos actualizados de un post único (IMAGE o VIDEO).
     * Usado para refrescar la media_url de imágenes simples que han expirado.
     * Devuelve {@link java.util.Optional#empty()} ante cualquier error de red (fail-safe).
     */
    public java.util.Optional<InstagramMediaItem> fetchSingleMedia(String mediaId) {
        String url = UriComponentsBuilder
            .fromPath("/{mediaId}")
            .queryParam("fields",       MEDIA_FIELDS)
            .queryParam("access_token", tokenProvider.getCurrentToken())
            .buildAndExpand(mediaId)
            .toUriString();

        try {
            InstagramMediaItem item = restClient.get()
                .uri(url)
                .retrieve()
                .body(InstagramMediaItem.class);
            return java.util.Optional.ofNullable(item);
        } catch (RestClientException e) {
            log.warn("Error obteniendo media {}: {}", mediaId, e.getMessage());
            return java.util.Optional.empty();
        }
    }

    // ── Privados ────────────────────────────────────────────────────

    private String buildInitialUrl(long sinceTimestamp) {
        return UriComponentsBuilder
            .fromPath("/{accountId}/media")
            .queryParam("fields",       MEDIA_FIELDS)
            .queryParam("since",        sinceTimestamp)
            .queryParam("access_token", tokenProvider.getCurrentToken())
            .buildAndExpand(accountId)
            .toUriString();
    }
}
