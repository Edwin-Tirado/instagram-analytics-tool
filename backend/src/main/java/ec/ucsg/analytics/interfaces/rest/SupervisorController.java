package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.application.dto.request.ApprovalRequest;
import ec.ucsg.analytics.application.dto.request.UpdateEventRequest;
import ec.ucsg.analytics.application.dto.response.EventResponse;
import ec.ucsg.analytics.application.dto.response.IngestionRunResponse;
import ec.ucsg.analytics.application.dto.response.PageResponse;
import ec.ucsg.analytics.application.service.EventIngestionService;
import ec.ucsg.analytics.application.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Panel de gestión de eventos — acceso exclusivo a SUPERVISOR y ADMIN.
 *
 * Los supervisores pueden ver, editar, eliminar eventos publicados (APPROVED)
 * y además APROBAR o RECHAZAR eventos PENDING. Toda acción queda auditada
 * con el ID del supervisor a través de principal.getUsername().
 *
 * Rutas:
 *   GET    /api/supervisor/events               → lista paginada de eventos publicados
 *   GET    /api/supervisor/events/{id}          → detalle completo
 *   PUT    /api/supervisor/events/{id}          → editar título, fecha, zona
 *   DELETE /api/supervisor/events/{id}          → eliminar evento publicado
 *   POST   /api/supervisor/events/{id}/approve  → aprobar evento PENDING (registra auditoría)
 *   POST   /api/supervisor/events/{id}/reject   → rechazar evento PENDING (registra auditoría)
 *   GET    /api/supervisor/events/ingestion-runs → historial de ingesta (lectura)
 */
@Slf4j
@RestController
@RequestMapping("/api/supervisor/events")
@PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
@RequiredArgsConstructor
public class SupervisorController {

    private final EventService          eventService;
    private final EventIngestionService ingestionService;

    @GetMapping
    public ResponseEntity<PageResponse<EventResponse>> getAllEventsForReview(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // El supervisor ve TODOS los estados (PENDING, APPROVED, REJECTED)
        // para poder revisar y actuar sobre los eventos pendientes.
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
            PageResponse.of(eventService.getAllEventsForSupervisor(pageable))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getSupervisorEventById(id));
    }

    /**
     * Edita un evento publicado.
     * Solo se actualizan los campos enviados en el body (patch semántico).
     * El email del supervisor queda registrado en el log de auditoría.
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

    /**
     * Aprueba un evento PENDING.
     *
     * La acción queda registrada en event.reviewedBy con el ID del supervisor
     * y event.reviewedAt con el timestamp de la decisión.
     *
     * Acceso: ROLE_SUPERVISOR y ROLE_ADMIN.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<EventResponse> approveEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {

        log.info("Supervisor '{}' aprobando evento id={}", principal.getUsername(), id);
        return ResponseEntity.ok(
            eventService.approveEvent(id, principal.getUsername())
        );
    }

    /**
     * Rechaza un evento PENDING con un motivo opcional.
     *
     * La acción queda registrada en event.reviewedBy, event.reviewedAt
     * y event.rejectionReason.
     *
     * Acceso: ROLE_SUPERVISOR y ROLE_ADMIN.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<EventResponse> rejectEvent(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        String reason = (request != null) ? request.reason() : null;
        log.info("Supervisor '{}' rechazando evento id={} — motivo: {}",
            principal.getUsername(), id, reason != null ? reason : "(sin motivo)");
        return ResponseEntity.ok(
            eventService.rejectEvent(id, reason, principal.getUsername())
        );
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
