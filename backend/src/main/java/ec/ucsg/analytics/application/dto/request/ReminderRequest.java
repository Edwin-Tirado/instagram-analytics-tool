package ec.ucsg.analytics.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Solicitud de creación de un recordatorio.
 *
 * {@code minutesBefore} acepta cualquier valor entre 1 y 10 080 (1 semana).
 * El frontend presentará opciones estándar: 15, 30, 60, 1440.
 */
public record ReminderRequest(
    @NotNull UUID eventId,
    @Min(1) @Max(10_080) int minutesBefore
) {}
