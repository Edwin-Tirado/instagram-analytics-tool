package ec.ucsg.analytics.application.mapper;

import ec.ucsg.analytics.application.dto.response.EventResponse;
import ec.ucsg.analytics.application.dto.response.EventSummaryResponse;
import ec.ucsg.analytics.domain.model.Event;
import java.time.LocalDateTime;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-22T15:38:47-0500",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class EventMapperImpl implements EventMapper {

    @Override
    public EventResponse toResponse(Event event) {
        if ( event == null ) {
            return null;
        }

        String title = null;
        String caption = null;
        String locationText = null;
        LocalDateTime eventDate = null;
        String status = null;
        String rejectionReason = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        title = event.getTitle();
        caption = event.getCaption();
        locationText = event.getLocationText();
        eventDate = event.getEventDate();
        if ( event.getStatus() != null ) {
            status = event.getStatus().name();
        }
        rejectionReason = event.getRejectionReason();
        createdAt = event.getCreatedAt();
        updatedAt = event.getUpdatedAt();

        String id = event.getId().toString();
        EventResponse.ZoneInfo zone = toZoneInfo(event.getZone());
        List<String> imageUrls = extractImageUrls(event);
        EventResponse.ReviewInfo review = toReviewInfo(event);

        EventResponse eventResponse = new EventResponse( id, title, caption, locationText, zone, eventDate, status, imageUrls, rejectionReason, review, createdAt, updatedAt );

        return eventResponse;
    }

    @Override
    public EventSummaryResponse toSummary(Event event) {
        if ( event == null ) {
            return null;
        }

        String title = null;
        LocalDateTime eventDate = null;

        title = event.getTitle();
        eventDate = event.getEventDate();

        String id = event.getId().toString();
        String status = event.getStatus().name();
        String zoneName = event.getZone() != null ? event.getZone().getName() : null;
        Double latitude = extractLatitude(event.getZone());
        Double longitude = extractLongitude(event.getZone());
        String thumbnailUrl = extractThumbnail(event);

        EventSummaryResponse eventSummaryResponse = new EventSummaryResponse( id, title, status, eventDate, zoneName, latitude, longitude, thumbnailUrl );

        return eventSummaryResponse;
    }
}
