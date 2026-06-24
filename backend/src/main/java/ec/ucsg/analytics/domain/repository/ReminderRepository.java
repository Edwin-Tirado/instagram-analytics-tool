package ec.ucsg.analytics.domain.repository;

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
     * Recordatorios pendientes cuya ventana ya venció:
     *   event_date - (minutes_before MINUTES) <= :now
     *
     * Query nativa para compatibilidad con el operador de intervalo de PostgreSQL.
     */
    @Query(value = """
        SELECT r.* FROM reminders r
        JOIN events e ON r.event_id = e.id
        WHERE r.sent = false
          AND e.event_date - (r.minutes_before * INTERVAL '1 minute') <= :now
        """, nativeQuery = true)
    List<Reminder> findDueReminders(@Param("now") LocalDateTime now);
}
