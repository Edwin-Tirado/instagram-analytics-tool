package ec.ucsg.analytics.infrastructure.scheduling;

import ec.ucsg.analytics.application.service.EventIngestionService;
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
        log.info("=== Inicio del job de ingesta de Instagram ===");
        try {
            ingestionService.runIngestion();
        } catch (Exception e) {
            log.error("El job de ingesta terminó con error: {}", e.getMessage(), e);
        }
        log.info("=== Fin del job de ingesta de Instagram ===");
    }
}
