package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.application.dto.response.EventResponse;
import ec.ucsg.analytics.application.dto.response.EventSummaryResponse;
import ec.ucsg.analytics.application.dto.response.PageResponse;
import ec.ucsg.analytics.application.dto.response.ZoneResponse;
import ec.ucsg.analytics.application.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints públicos para la vista del mapa.
 *
 * Consumidos por el frontend Next.js sin necesidad de JWT.
 * Protegidos solo por el RateLimitingFilter (60 req/min por IP).
 *
 * Rutas:
 *   GET /api/public/events       → marcadores del mapa (paginado)
 *   GET /api/public/events/{id}  → detalle con carrusel de imágenes
 *   GET /api/public/zones        → zonas del campus con coordenadas
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * Devuelve los eventos APPROVED próximos, ordenados por fecha.
     * Usado para renderizar los marcadores/pins en el mapa Mapbox.
     *
     * @param page  número de página (0-based, default 0)
     * @param size  elementos por página (default 50, max 100)
     */
    @GetMapping("/events")
    public ResponseEntity<PageResponse<EventSummaryResponse>> getEventsForMap(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        size = Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("eventDate").ascending());
        return ResponseEntity.ok(
            PageResponse.of(eventService.getApprovedForMap(pageable))
        );
    }

    /**
     * Detalle completo de un evento: datos, carrusel de imágenes y zona.
     * Solo devuelve eventos en estado APPROVED.
     */
    @GetMapping("/events/{id}")
    public ResponseEntity<EventResponse> getEventDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    /**
     * Lista todas las zonas del campus con sus coordenadas.
     * El frontend las usa para renderizar el layer de zonas en el mapa.
     */
    @GetMapping("/zones")
    public ResponseEntity<List<ZoneResponse>> getAllZones() {
        return ResponseEntity.ok(eventService.getAllZones());
    }
}
