package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.application.dto.response.IngestionRunResponse;
import ec.ucsg.analytics.application.dto.response.PageResponse;
import ec.ucsg.analytics.application.service.EventIngestionService;
import ec.ucsg.analytics.domain.model.IngestionRun;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Panel de administración del job de ingesta de Instagram.
 * Acceso exclusivo: ROLE_ADMIN.
 *
 * Rutas:
 *   POST /api/admin/ingestion/run          → disparo manual inmediato
 *   POST /api/admin/ingestion/refresh-urls → refresca todas las URLs expiradas
 *   GET  /api/admin/ingestion/runs         → historial paginado (más reciente primero)
 *   GET  /api/admin/ingestion/runs/{id}    → detalle de una ejecución específica
 */
@RestController
@RequestMapping("/api/admin/ingestion")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminIngestionController {

    private final EventIngestionService ingestionService;

    /**
     * Dispara la ingesta manualmente sin esperar el CRON.
     * Asíncrono: crea el registro RUNNING y responde de inmediato mientras el
     * pipeline (llamadas a la API de Instagram) corre en segundo plano. El
     * frontend hace polling de GET /runs/{id} hasta que status ya no sea RUNNING.
     */
    @PostMapping("/run")
    public ResponseEntity<IngestionRunResponse> triggerManualIngestion() {
        IngestionRun run = ingestionService.createRun(IngestionRun.TriggerType.MANUAL);
        ingestionService.executeIngestionAsync(run.getId());
        return ResponseEntity.ok(IngestionRunResponse.from(run));
    }

    /**
     * Refresca las URLs CDN de todas las imágenes almacenadas que puedan haber
     * expirado (Instagram CDN expira las URLs en ~7 días).
     *
     * Retorna 202 Accepted de inmediato; el trabajo real corre en segundo plano
     * (útil para cuentas con muchos eventos históricos fuera de la ventana del CRON).
     * El progreso queda registrado en los logs del servidor.
     */
    @PostMapping("/refresh-urls")
    public ResponseEntity<Map<String, String>> triggerUrlRefresh() {
        ingestionService.refreshAllExpiredUrlsAsync();
        return ResponseEntity.accepted()
            .body(Map.of(
                "status",  "ACCEPTED",
                "message", "Refresco de URLs iniciado en segundo plano. Revisa los logs del servidor para ver el progreso."
            ));
    }

    @GetMapping("/runs")
    public ResponseEntity<PageResponse<IngestionRunResponse>> getRunHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ingestionService.getRunHistory(pageable));
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<IngestionRunResponse> getRunById(@PathVariable UUID id) {
        return ResponseEntity.ok(ingestionService.getRunById(id));
    }
}
