package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.application.dto.request.UpdateEventRequest;
import ec.ucsg.analytics.application.dto.response.EventResponse;
import ec.ucsg.analytics.application.dto.response.IngestionRunResponse;
import ec.ucsg.analytics.application.dto.response.PageResponse;
import ec.ucsg.analytics.application.service.EventIngestionService;
import ec.ucsg.analytics.application.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Panel de gestión de eventos publicados — acceso exclusivo a SUPERVISOR y ADMIN.
 *
 * Los eventos se publican automáticamente desde Instagram (estado APPROVED).
 * El supervisor puede consultarlos, editarlos o eliminarlos.
 *
 * Rutas:
 *   GET    /api/supervisor/events        → lista paginada de eventos publicados
 *   GET    /api/supervisor/events/{id}   → detalle completo
 *   PUT    /api/supervisor/events/{id}   → editar título, fecha, zona
 *   DELETE /api/supervisor/events/{id}   → eliminar evento publicado
 */
@RestController
@RequestMapping("/api/supervisor/events")
@PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
@RequiredArgsConstructor
public class SupervisorController {

    private final EventService        eventService;
    private final EventIngestionService ingestionService;

    @GetMapping
    public ResponseEntity<PageResponse<EventResponse>> getPublishedEvents(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());
        return ResponseEntity.ok(
            PageResponse.of(eventService.getApprovedEventsForSupervisor(pageable))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getSupervisorEventById(id));
    }

    /**
     * Edita un evento publicado.
     * Solo se actualizan los campos enviados en el body (patch semántico).
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        return ResponseEntity.ok(
            eventService.updateEvent(id, request, principal.getUsername())
        );
    }

    /** Elimina permanentemente un evento publicado y su carrusel de imágenes. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {

        eventService.deleteEvent(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    // ── Historial de ingesta (lectura) ───────────────────────────────

    /**
     * Historial de ejecuciones del job de Instagram — lectura solo para supervisores.
     * El disparo manual sigue siendo exclusivo del admin (POST /api/admin/ingestion/run).
     */
    @GetMapping("/ingestion-runs")
    public ResponseEntity<PageResponse<IngestionRunResponse>> getIngestionHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ingestionService.getRunHistory(pageable));
    }
}
