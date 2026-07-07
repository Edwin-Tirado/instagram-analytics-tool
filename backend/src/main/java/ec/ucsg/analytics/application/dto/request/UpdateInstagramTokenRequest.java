package ec.ucsg.analytics.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload para que un admin pegue manualmente el access-token de Instagram. */
public record UpdateInstagramTokenRequest(

    @NotBlank(message = "El access token es obligatorio")
    @Size(max = 500, message = "El access token no puede superar los 500 caracteres")
    String accessToken

) {}
