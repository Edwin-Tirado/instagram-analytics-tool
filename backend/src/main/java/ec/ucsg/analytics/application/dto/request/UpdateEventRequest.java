package ec.ucsg.analytics.application.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Payload para que el supervisor edite un evento ya publicado.
 * Todos los campos son opcionales — solo se actualizan los no-nulos.
 *
 * eventDate NO exige @Future: el formulario de edición siempre reenvía la
 * fecha actual del evento (se haya tocado o no), así que un evento ya
 * pasado nunca podría guardarse — ni siquiera para corregirle el texto de
 * ubicación. Corregir la fecha de un evento histórico también es válido.
 */
public record UpdateEventRequest(

    @Size(max = 255, message = "El título no puede superar los 255 caracteres")
    String title,

    LocalDateTime eventDate,

    Long zoneId,

    @Size(max = 255, message = "El texto de ubicación no puede superar los 255 caracteres")
    String locationText

) {}
