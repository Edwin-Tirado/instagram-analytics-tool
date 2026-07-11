package ec.ucsg.analytics.application.dto.response;

import ec.ucsg.analytics.domain.enums.AuditAction;
import ec.ucsg.analytics.domain.model.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de auditoría — usado tanto para el historial de aprobaciones/rechazos
 * como para el nuevo registro de ediciones/eliminaciones.
 */
public record AuditLogResponse(
    UUID          id,
    UUID          eventId,
    String        eventTitle,
    String        supervisorEmail,
    String        supervisorName,
    AuditAction   action,
    LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        String title = log.getEvent() != null
            ? log.getEvent().getTitle()
            : (log.getEventTitleSnapshot() != null ? log.getEventTitleSnapshot() : "(evento eliminado)");

        return new AuditLogResponse(
            log.getId(),
            log.getEvent() != null ? log.getEvent().getId() : null,
            title,
            log.getSupervisor() != null ? log.getSupervisor().getEmail()    : null,
            log.getSupervisor() != null ? log.getSupervisor().getFullName() : null,
            log.getAction(),
            log.getCreatedAt()
        );
    }
}
