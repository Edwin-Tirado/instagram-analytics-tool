package ec.ucsg.analytics.infrastructure.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Representa un post individual de la Graph API de Meta.
 * Campos solicitados en el parámetro ?fields= de la petición.
 */
@Data
@NoArgsConstructor
public class InstagramMediaItem {

    private String id;

    private String caption;

    /** IMAGE | VIDEO | CAROUSEL_ALBUM */
    @JsonProperty("media_type")
    private String mediaType;

    /**
     * URL pública de la imagen/video.
     * Para CAROUSEL_ALBUM el padre no tiene media_url directa — se usa
     * la colección de hijos obtenida con fetchChildren().
     */
    @JsonProperty("media_url")
    private String mediaUrl;

    /** URL permanente del post en Instagram (útil para auditoría). */
    private String permalink;

    /** ISO-8601 UTC — "2026-06-20T15:30:00+0000" */
    private String timestamp;

    /**
     * Hijos del carrusel — presente solo cuando se solicita el campo
     * "children{id,media_url,media_type}" en la petición de hijos.
     */
    private InstagramChildrenResponse children;

    /**
     * Categoría de contenido de Meta (BRANDED_CONTENT, etc.)
     * Opcional — no siempre está presente.
     */
    @JsonProperty("media_product_type")
    private String mediaProductType;
}
