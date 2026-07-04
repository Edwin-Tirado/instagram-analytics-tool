package ec.ucsg.analytics.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Access token de Instagram vigente — fila única (id=1). Se guarda en BD
 * (no solo en application.yml) para poder renovarlo en caliente vía
 * {@code InstagramTokenProvider} sin reiniciar el backend.
 */
@Entity
@Table(name = "instagram_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramToken {

    @Id
    private Integer id;

    @Column(name = "access_token", nullable = false, length = 500)
    private String accessToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
