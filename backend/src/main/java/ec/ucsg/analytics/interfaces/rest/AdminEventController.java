package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.application.dto.request.ApprovalRequest;
import ec.ucsg.analytics.application.dto.request.UpdateEventRequest;
import ec.ucsg.analytics.application.dto.response.EventResponse;
import ec.ucsg.analytics.application.dto.response.PageResponse;
import ec.ucsg.analytics.application.service.EventService;
import ec.ucsg.analytics.domain.enums.EventStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Panel de administración de eventos — acceso exclusivo a ROLE_ADMIN.
 *
 * A diferencia del supervisor (solo ve APPROVED), el admin ve TODOS los estados.
 *
 * Rutas:
 *   GET    /api/admin/events              → lista paginada, filtra por status
 *   GET    /api/admin/events/{id}         → detalle completo
 *   PUT    /api/admin/events/{id}         → editar título, fecha, zona
 *   DELETE /api/admin/events/{id}         → eliminar permanentemente
 *   POST   /api/admin/events/{id}/approve → publicar evento PENDING
 *   POST   /api/admin/events/{id}/reject  → rechazar evento PENDING
 */
@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<PageResponse<EventResponse>> getAllEvents(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(required = false)     String status) {

        EventStatus filter = null;
        if (status != null && !status.isBlank()) {
            try {
                filter = EventStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(PageResponse.of(eventService.getAllEventsForAdmin(filter, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getSupervisorEventById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        return ResponseEntity.ok(eventService.updateEvent(id, request, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {

        eventService.deleteEvent(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<EventResponse> approveEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {

        return ResponseEntity.ok(eventService.approveEvent(id, principal.getUsername()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<EventResponse> rejectEvent(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        String reason = (request != null) ? request.reason() : null;
        return ResponseEntity.ok(eventService.rejectEvent(id, reason, principal.getUsername()));
    }
}
