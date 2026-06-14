package ec.ucsg.analytics.domain.model;

import ec.ucsg.analytics.domain.enums.EventStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Evento universitario centralizado.
 *
 * Flujo de vida:
 *   Instagram @ucsgnotificaciones  →  PENDING  →  APPROVED / REJECTED
 *
 * Lógica antiduplicados (regla de negocio):
 *   Si ya existe un evento con mismo título + fecha + zona,
 *   no se crea uno nuevo — se añade la imagen al carrusel existente.
 */
@Entity
@Table(
    name = "events",
    indexes = {
        @Index(name = "idx_events_status",       columnList = "status"),
        @Index(name = "idx_events_event_date",   columnList = "event_date"),
        @Index(name = "idx_events_instagram_id", columnList = "instagram_post_id", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "title", "status"})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Texto completo del post de Instagram (caption).
     * Fuente de información para el auto-matching de zona.
     */
    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    /**
     * Texto de ubicación extraído del caption o etiqueta de Instagram.
     * Campo libre — la zona estructurada se resuelve en {@link Zone}.
     */
    @Column(name = "location_text", length = 255)
    private String locationText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.PENDING;

    /** ID del post original en la Graph API de Meta. */
    @Column(name = "instagram_post_id", length = 50, unique = true)
    private String instagramPostId;

    // ── Relaciones ──────────────────────────────────────────────────

    /** Carrusel de imágenes (≥1 imagen por evento). */
    @OneToMany(
        mappedBy     = "event",
        cascade      = CascadeType.ALL,
        orphanRemoval = true,
        fetch        = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<EventImage> images = new ArrayList<>();

    @OneToMany(
        mappedBy = "event",
        cascade  = CascadeType.ALL,
        fetch    = FetchType.LAZY
    )
    @Builder.Default
    private List<Reminder> reminders = new ArrayList<>();

    // ── Revisión por supervisor ─────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private AppUser reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // ── Auditoría ───────────────────────────────────────────────────

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Métodos de dominio ──────────────────────────────────────────

    public void approve(AppUser supervisor) {
        this.status      = EventStatus.APPROVED;
        this.reviewedBy  = supervisor;
        this.reviewedAt  = LocalDateTime.now();
    }

    public void reject(AppUser supervisor, String reason) {
        this.status          = EventStatus.REJECTED;
        this.reviewedBy      = supervisor;
        this.reviewedAt      = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public void addImage(EventImage image) {
        image.setEvent(this);
        this.images.add(image);
    }
}
