package ec.ucsg.analytics.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReminderResponse(
    UUID          id,
    UUID          eventId,
    String        eventTitle,
    LocalDateTime eventDate,
    int           minutesBefore,
    boolean       sent,
    LocalDateTime createdAt
) {}
