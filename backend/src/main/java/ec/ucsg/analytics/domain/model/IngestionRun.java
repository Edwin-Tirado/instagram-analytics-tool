package ec.ucsg.analytics.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de una ejecución del job de ingesta de Instagram.
 *
 * Ciclo de vida:
 *   RUNNING → SUCCESS (ejecución normal)
 *   RUNNING → FAILED  (error no recuperado en el pipeline)
 *
 * Los contadores reflejan el resultado agregado de todos los posts
 * procesados en esa ejecución.
 */
@Entity
@Table(
    name = "ingestion_runs",
    indexes = {
        @Index(name = "idx_ingestion_runs_started_at", columnList = "started_at"),
        @Index(name = "idx_ingestion_runs_status",     columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private TriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RunStatus status = RunStatus.RUNNING;

    @Column(name = "created_count", nullable = false)
    @Builder.Default
    private int createdCount = 0;

    @Column(name = "merged_count", nullable = false)
    @Builder.Default
    private int mergedCount = 0;

    @Column(name = "rejected_count", nullable = false)
    @Builder.Default
    private int rejectedCount = 0;

    @Column(name = "skipped_count", nullable = false)
    @Builder.Default
    private int skippedCount = 0;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // ── Métodos de dominio ──────────────────────────────────────────

    public void complete(int created, int merged, int rejected, int skipped) {
        this.createdCount  = created;
        this.mergedCount   = merged;
        this.rejectedCount = rejected;
        this.skippedCount  = skipped;
        this.status        = RunStatus.SUCCESS;
        this.endedAt       = LocalDateTime.now();
    }

    public void fail(String message) {
        this.status       = RunStatus.FAILED;
        this.endedAt      = LocalDateTime.now();
        this.errorMessage = message != null && message.length() > 500
            ? message.substring(0, 500)
            : message;
    }

    // ── Enums internos ──────────────────────────────────────────────

    public enum TriggerType { SCHEDULED, MANUAL }

    public enum RunStatus   { RUNNING, SUCCESS, FAILED }
}
