package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.application.dto.request.ReminderRequest;
import ec.ucsg.analytics.application.dto.response.ReminderResponse;
import ec.ucsg.analytics.application.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gestión de recordatorios personales de eventos.
 * Acceso: cualquier usuario autenticado (ROLE_USER, SUPERVISOR, ADMIN).
 *
 * Rutas:
 *   GET    /api/reminders        → lista mis recordatorios activos
 *   POST   /api/reminders        → crear recordatorio para un evento
 *   DELETE /api/reminders/{id}   → eliminar un recordatorio propio
 */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    /** Endpoint público de diagnóstico — eliminar en producción */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ReminderController activo");
    }

    @GetMapping
    public ResponseEntity<List<ReminderResponse>> getMyReminders(
            @AuthenticationPrincipal UserDetails principal) {

        return ResponseEntity.ok(reminderService.getUserReminders(principal.getUsername()));
    }

    @PostMapping
    public ResponseEntity<ReminderResponse> createReminder(
            @Valid @RequestBody ReminderRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        ReminderResponse response = reminderService.createReminder(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {

        reminderService.deleteReminder(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
