package ec.ucsg.analytics.infrastructure.scheduling;

import ec.ucsg.analytics.application.service.EventIngestionService;
import ec.ucsg.analytics.domain.model.IngestionRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CRON Job de ingesta de posts de Instagram @ucsgnotificaciones.
 *
 * Frecuencia configurable en application.yml:
 *   app.instagram.cron-expression (default: cada 2 horas)
 *
 * La lógica de negocio completa está delegada en {@link EventIngestionService}
 * para mantener este componente delgado y testeable de forma independiente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramIngestionJob {

    private final EventIngestionService ingestionService;

    @Scheduled(cron = "${app.instagram.cron-expression}")
    public void ingestInstagramPosts() {
        log.info("=== Inicio del job de ingesta de Instagram (SCHEDULED) ===");
        ingestionService.runIngestion(IngestionRun.TriggerType.SCHEDULED);
        log.info("=== Fin del job de ingesta de Instagram ===");
    }
}
