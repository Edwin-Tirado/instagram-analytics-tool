package ec.ucsg.analytics.infrastructure.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta de GET /refresh_access_token (grant_type=ig_refresh_token). */
public record InstagramTokenRefreshResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type")   String tokenType,
    @JsonProperty("expires_in")   long   expiresIn
) {}
