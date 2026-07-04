package ec.ucsg.analytics.infrastructure.instagram;

/**
 * Señala que una llamada a la Instagram Graph API falló (token inválido/expirado,
 * cuenta desvinculada, error de red, etc.). Se deja propagar hasta
 * {@code EventIngestionService}, que marca el {@code IngestionRun} como FAILED
 * con el mensaje real — en vez de reportar éxito con cero posts.
 */
public class InstagramApiException extends RuntimeException {
    public InstagramApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
