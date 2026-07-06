package ec.ucsg.analytics.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload para que el supervisor/admin agregue una nueva ubicación (zona) del campus. */
public record CreateZoneRequest(

    @NotBlank(message = "El nombre de la ubicación es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    String name,

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90",  message = "Latitud fuera de rango")
    @DecimalMax(value = "90",   message = "Latitud fuera de rango")
    Double latitude,

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180", message = "Longitud fuera de rango")
    @DecimalMax(value = "180",  message = "Longitud fuera de rango")
    Double longitude

) {}
