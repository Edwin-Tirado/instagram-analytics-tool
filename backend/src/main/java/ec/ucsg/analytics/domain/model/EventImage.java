package ec.ucsg.analytics.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Imagen individual dentro del carrusel de un evento.
 *
 * Un mismo evento físico puede aparecer en múltiples posts de Instagram
 * (lógica antiduplicados). Cada post nuevo aporta una imagen adicional
 * a esta colección, que el frontend renderiza como carrusel.
 */
@Entity
@Table(
    name = "event_images",
    indexes = {
        @Index(name = "idx_event_images_event_id",      columnList = "event_id"),
        @Index(name = "idx_event_images_instagram_post", columnList = "source_instagram_post_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "mediaUrl", "displayOrder"})
public class EventImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotBlank
    @Column(name = "media_url", nullable = false, length = 1024)
    private String mediaUrl;

    /**
     * ID del post de Instagram que originó esta imagen.
     * Permite rastrear la fuente y evitar re-ingestión.
     */
    @Column(name = "source_instagram_post_id", length = 50)
    private String sourceInstagramPostId;

    /** Orden de aparición en el carrusel del frontend (0-based). */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
