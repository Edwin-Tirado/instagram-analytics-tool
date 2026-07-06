package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.application.dto.request.CreateZoneRequest;
import ec.ucsg.analytics.application.dto.response.ZoneResponse;
import ec.ucsg.analytics.application.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de ubicaciones (zonas) del campus — acceso SUPERVISOR y ADMIN.
 *
 * Rutas:
 *   POST /api/supervisor/zones → crea una nueva ubicación (nombre + coordenadas)
 */
@RestController
@RequestMapping("/api/supervisor/zones")
@PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
@RequiredArgsConstructor
public class SupervisorZoneController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ZoneResponse> createZone(@Valid @RequestBody CreateZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createZone(request));
    }
}
