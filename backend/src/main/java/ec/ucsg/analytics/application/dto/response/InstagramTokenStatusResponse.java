package ec.ucsg.analytics.application.dto.response;

import java.time.LocalDateTime;

/** Estado del token de Instagram — el valor real nunca se expone, solo los últimos 4 caracteres. */
public record InstagramTokenStatusResponse(
    boolean       configured,
    String        maskedToken,
    LocalDateTime updatedAt,
    LocalDateTime expiresAt
) {}
