package ec.ucsg.analytics.application.dto.response;

import java.time.LocalDateTime;

/**
 * Versión ligera de evento para los marcadores del mapa.
 * Solo incluye los campos estrictamente necesarios para renderizar un pin.
 */
public record EventSummaryResponse(
    String        id,
    String        title,
    String        status,
    LocalDateTime eventDate,
    String        zoneName,
    Double        latitude,
    Double        longitude,
    String        thumbnailUrl    // primera imagen del carrusel
) {}
