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

import java.util.UUID;

/**
 * Panel de administración del job de ingesta de Instagram.
 * Acceso exclusivo: ROLE_ADMIN.
 *
 * Rutas:
 *   POST /api/admin/ingestion/run        → disparo manual inmediato
 *   GET  /api/admin/ingestion/runs       → historial paginado (más reciente primero)
 *   GET  /api/admin/ingestion/runs/{id}  → detalle de una ejecución específica
 */
@RestController
@RequestMapping("/api/admin/ingestion")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminIngestionController {

    private final EventIngestionService ingestionService;

    /**
     * Dispara la ingesta manualmente sin esperar el CRON.
     * La llamada es síncrona: devuelve la respuesta cuando el pipeline finaliza.
     */
    @PostMapping("/run")
    public ResponseEntity<IngestionRunResponse> triggerManualIngestion() {
        IngestionRun run = ingestionService.runIngestion(IngestionRun.TriggerType.MANUAL);
        return ResponseEntity.ok(IngestionRunResponse.from(run));
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
