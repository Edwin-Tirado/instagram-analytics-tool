package ec.ucsg.analytics.application.dto.response;

import java.util.Set;

/**
 * Respuesta JWT devuelta en login y register.
 *
 * El frontend almacena accessToken en memoria (nunca en localStorage).
 * El refreshToken puede guardarse en una cookie HttpOnly.
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long   expiresIn,       // segundos hasta que vence el access token
    UserInfo user
) {
    public record UserInfo(
        String      id,
        String      email,
        String      fullName,
        Set<String> roles
    ) {}

    public static AuthResponse of(
            String accessToken,
            String refreshToken,
            long   expiresIn,
            UserInfo user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
