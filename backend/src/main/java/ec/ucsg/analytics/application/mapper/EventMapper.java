package ec.ucsg.analytics.application.mapper;

import ec.ucsg.analytics.application.dto.response.EventResponse;
import ec.ucsg.analytics.application.dto.response.EventSummaryResponse;
import ec.ucsg.analytics.domain.model.Event;
import ec.ucsg.analytics.domain.model.EventImage;
import ec.ucsg.analytics.domain.model.Zone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    // Ubicación real del campus Guayaquil de la UCSG — verificada por búsqueda
    // web (Av. Carlos Julio Arosemena Km 1.5) y consistente con el centroide
    // de las zonas ya corregidas en V10__fix_original_zone_coordinates.sql.
    // Respaldo cuando un evento no tiene zona específica asignada, para que
    // el mapa SIEMPRE muestre algo en vez de quedar oculto/vacío.
    double DEFAULT_CAMPUS_LATITUDE  = -2.181827;
    double DEFAULT_CAMPUS_LONGITUDE = -79.904600;
    String DEFAULT_CAMPUS_NAME      = "Campus UCSG";

    // ── EventResponse (detalle completo) ────────────────────────────

    @Mapping(target = "id",             expression = "java(event.getId().toString())")
    @Mapping(target = "zone",           expression = "java(toZoneInfo(event.getZone()))")
    @Mapping(target = "imageUrls",      expression = "java(extractImageUrls(event))")
    @Mapping(target = "review",         expression = "java(toReviewInfo(event))")
    EventResponse toResponse(Event event);

    // ── EventSummaryResponse (marcador de mapa) ──────────────────────

    @Mapping(target = "id",           expression = "java(event.getId().toString())")
    @Mapping(target = "status",       expression = "java(event.getStatus().name())")
    @Mapping(target = "zoneName",     expression = "java(event.getZone() != null ? event.getZone().getName() : DEFAULT_CAMPUS_NAME)")
    @Mapping(target = "latitude",     expression = "java(extractLatitude(event.getZone()))")
    @Mapping(target = "longitude",    expression = "java(extractLongitude(event.getZone()))")
    @Mapping(target = "thumbnailUrl", expression = "java(extractThumbnail(event))")
    EventSummaryResponse toSummary(Event event);

    // ── Métodos default de apoyo ─────────────────────────────────────

    default EventResponse.ZoneInfo toZoneInfo(Zone zone) {
        if (zone == null) {
            return new EventResponse.ZoneInfo(null, DEFAULT_CAMPUS_NAME, DEFAULT_CAMPUS_LATITUDE, DEFAULT_CAMPUS_LONGITUDE);
        }
        return new EventResponse.ZoneInfo(
            zone.getId(),
            zone.getName(),
            extractLatitude(zone),
            extractLongitude(zone)
        );
    }

    default EventResponse.ReviewInfo toReviewInfo(Event event) {
        if (event.getReviewedBy() == null) return null;
        return new EventResponse.ReviewInfo(
            event.getReviewedBy().getEmail(),
            event.getReviewedAt()
        );
    }

    default List<String> extractImageUrls(Event event) {
        if (event.getImages() == null) return List.of();
        return event.getImages().stream()
            .map(EventImage::getMediaUrl)
            .toList();
    }

    default String extractThumbnail(Event event) {
        if (event.getImages() == null || event.getImages().isEmpty()) return null;
        return event.getImages().get(0).getMediaUrl();
    }

    default Double extractLatitude(Zone zone) {
        if (zone == null || zone.getLocation() == null) return DEFAULT_CAMPUS_LATITUDE;
        return zone.getLocation().getY();   // Y = latitud en WGS84
    }

    default Double extractLongitude(Zone zone) {
        if (zone == null || zone.getLocation() == null) return DEFAULT_CAMPUS_LONGITUDE;
        return zone.getLocation().getX();   // X = longitud en WGS84
    }
}
