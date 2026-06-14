package ec.ucsg.analytics.infrastructure.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job de ingesta de posts desde Instagram @ucsgnotificaciones.
 *
 * Implementación completa en Sprint 2 (Instagram Graph API client).
 * Esta clase define la estructura del scheduler y sus puntos de extensión.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramIngestionJob {

    @Value("${app.instagram.cron-expression}")
    private String cronExpression;

    @Value("${app.instagram.days-lookback:30}")
    private int daysLookback;

    /**
     * El cron se lee de application.yml para que sea configurable
     * por entorno sin recompilar.
     *
     * Flujo completo (Sprint 2):
     * 1. Llamar a InstagramGraphApiClient.fetchRecentPosts(daysLookback)
     * 2. Por cada post → EventIngestionService.ingest(postDto)
     *    a. Parsear título y fecha del caption.
     *    b. Auto-asignar zona por keyword matching.
     *    c. Detectar duplicado (título + zona + fecha ±1h).
     *    d. Si duplicado: añadir imagen al carrusel existente.
     *    e. Si nuevo: crear Event en estado PENDING + primera EventImage.
     */
    @Scheduled(cron = "${app.instagram.cron-expression}")
    public void ingestInstagramPosts() {
        log.info("Iniciando ingesta de posts de Instagram (últimos {} días)...", daysLookback);
        // TODO Sprint 2: implementar llamada a la Graph API
        log.info("Ingesta finalizada.");
    }
}
