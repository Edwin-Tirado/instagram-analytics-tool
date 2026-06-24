package ec.ucsg.analytics.application.dto.response;

import ec.ucsg.analytics.domain.model.IngestionRun;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista pública de una ejecución del job de ingesta.
 * {@code durationSeconds} es null si la ejecución aún está en curso (RUNNING).
 */
public record IngestionRunResponse(
    UUID          id,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    String        triggerType,
    String        status,
    int           createdCount,
    int           mergedCount,
    int           rejectedCount,
    int           skippedCount,
    String        errorMessage,
    Long          durationSeconds
) {
    public static IngestionRunResponse from(IngestionRun run) {
        Long duration = (run.getStartedAt() != null && run.getEndedAt() != null)
            ? Duration.between(run.getStartedAt(), run.getEndedAt()).toSeconds()
            : null;

        return new IngestionRunResponse(
            run.getId(),
            run.getStartedAt(),
            run.getEndedAt(),
            run.getTriggerType().name(),
            run.getStatus().name(),
            run.getCreatedCount(),
            run.getMergedCount(),
            run.getRejectedCount(),
            run.getSkippedCount(),
            run.getErrorMessage(),
            duration
        );
    }
}
