package ec.ucsg.analytics.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload para que el administrador renombre una ubicación (zona) del campus. */
public record UpdateZoneRequest(

    @NotBlank(message = "El nombre de la ubicación es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    String name

) {}
