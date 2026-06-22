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

    // ── EventResponse (detalle completo) ────────────────────────────

    @Mapping(target = "id",             expression = "java(event.getId().toString())")
    @Mapping(target = "zone",           expression = "java(toZoneInfo(event.getZone()))")
    @Mapping(target = "imageUrls",      expression = "java(extractImageUrls(event))")
    @Mapping(target = "review",         expression = "java(toReviewInfo(event))")
    EventResponse toResponse(Event event);

    // ── EventSummaryResponse (marcador de mapa) ──────────────────────

    @Mapping(target = "id",           expression = "java(event.getId().toString())")
    @Mapping(target = "status",       expression = "java(event.getStatus().name())")
    @Mapping(target = "zoneName",     expression = "java(event.getZone() != null ? event.getZone().getName() : null)")
    @Mapping(target = "latitude",     expression = "java(extractLatitude(event.getZone()))")
    @Mapping(target = "longitude",    expression = "java(extractLongitude(event.getZone()))")
    @Mapping(target = "thumbnailUrl", expression = "java(extractThumbnail(event))")
    EventSummaryResponse toSummary(Event event);

    // ── Métodos default de apoyo ─────────────────────────────────────

    default EventResponse.ZoneInfo toZoneInfo(Zone zone) {
        if (zone == null) return null;
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
        if (zone == null || zone.getLocation() == null) return null;
        return zone.getLocation().getY();   // Y = latitud en WGS84
    }

    default Double extractLongitude(Zone zone) {
        if (zone == null || zone.getLocation() == null) return null;
        return zone.getLocation().getX();   // X = longitud en WGS84
    }
}
