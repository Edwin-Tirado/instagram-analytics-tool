package ec.ucsg.analytics.application.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Payload para que el supervisor edite un evento ya publicado.
 * Todos los campos son opcionales — solo se actualizan los no-nulos.
 */
public record UpdateEventRequest(

    @Size(max = 255, message = "El título no puede superar los 255 caracteres")
    String title,

    @Future(message = "La fecha del evento debe ser futura")
    LocalDateTime eventDate,

    Long zoneId,

    @Size(max = 255, message = "El texto de ubicación no puede superar los 255 caracteres")
    String locationText

) {}
