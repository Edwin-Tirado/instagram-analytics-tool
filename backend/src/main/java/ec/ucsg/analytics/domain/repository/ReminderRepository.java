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
     * Recordatorios cuyo evento es HOY y cuyo email de "día del evento"
     * aún no fue enviado. Se dispara a partir de las 08:00 del día del evento.
     */
    @Query(value = """
        SELECT r.* FROM reminders r
        JOIN events e ON r.event_id = e.id
        WHERE r.day_reminder_sent = false
          AND DATE(e.event_date) = DATE(:now)
          AND :now >= DATE(:now) + TIME '08:00:00'
        """, nativeQuery = true)
    List<Reminder> findDayReminders(@Param("now") LocalDateTime now);
}
