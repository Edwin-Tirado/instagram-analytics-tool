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

    List<Reminder> findByUserIdAndEventId(UUID userId, UUID eventId);

    /**
     * Recupera los recordatorios pendientes cuya ventana de disparo
     * ya se cumplió: event.eventDate - minutesBefore <= ahora.
     * Usado por el scheduler de notificaciones.
     */
    @Query("""
        SELECT r FROM Reminder r
        JOIN r.event e
        WHERE r.sent = false
          AND FUNCTION('timestampadd', MINUTE, -r.minutesBefore, e.eventDate) <= :now
        """)
    List<Reminder> findDueReminders(@Param("now") LocalDateTime now);
}
