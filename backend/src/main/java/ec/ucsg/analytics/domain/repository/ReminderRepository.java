package ec.ucsg.analytics.domain.repository;

import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    List<Reminder> findByUserId(UUID userId);

    List<Reminder> findByUserIdAndEventId(UUID userId, UUID eventId);

    /** Número de recordatorios activos de un evento — para el badge del supervisor. */
    long countByEventId(UUID eventId);

    /** Todos los recordatorios de un evento — usado para notificar a los usuarios al editar/eliminar. */
    List<Reminder> findByEventId(UUID eventId);

    /**
     * Usuarios que alguna vez pusieron un recordatorio en un evento de esta zona
     * (excluyendo al propio evento) — usado como proxy de "interés" para notificar
     * sobre nuevos eventos aprobados en zonas que ya le importan al usuario.
     */
    @Query("""
        SELECT DISTINCT r.user FROM Reminder r
        WHERE r.event.zone.id = :zoneId
          AND r.event.id <> :excludeEventId
        """)
    List<AppUser> findDistinctInterestedUsersByZoneId(
        @Param("zoneId") Long zoneId,
        @Param("excludeEventId") UUID excludeEventId
    );

    /**
     * Recordatorios cuya ventana de X-minutos-antes ya venció y aún no fueron enviados.
     */
    @Query(value = """
        SELECT r.* FROM reminders r
        JOIN events e ON r.event_id = e.id
        WHERE r.sent = false
          AND e.event_date - (r.minutes_before * INTERVAL '1 minute') <= :now
        """, nativeQuery = true)
    List<Reminder> findDueReminders(@Param("now") LocalDateTime now);

    /**
     * Recordatorios cuya ventana de 3-días-antes ya venció y cuyo email fijo
     * de "3 días antes del evento" aún no fue enviado.
     */
    @Query(value = """
        SELECT r.* FROM reminders r
        JOIN events e ON r.event_id = e.id
        WHERE r.three_days_reminder_sent = false
          AND e.event_date - INTERVAL '3 days' <= :now
        """, nativeQuery = true)
    List<Reminder> findThreeDaysBeforeReminders(@Param("now") LocalDateTime now);
}
