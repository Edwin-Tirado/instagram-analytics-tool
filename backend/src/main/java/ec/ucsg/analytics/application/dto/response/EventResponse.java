package ec.ucsg.analytics.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** Vista completa de un evento — usada en el detalle y en el panel del supervisor. */
public record EventResponse(
    String        id,
    String        title,
    String        caption,
    String        locationText,
    ZoneInfo      zone,
    LocalDateTime eventDate,
    String        status,
    List<String>  imageUrls,
    String        rejectionReason,
    ReviewInfo    review,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public record ZoneInfo(
        Long   id,
        String name,
        Double latitude,
        Double longitude
    ) {}

    public record ReviewInfo(
        String        reviewerEmail,
        LocalDateTime reviewedAt
    ) {}
}
