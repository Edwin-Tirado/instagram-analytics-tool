package ec.ucsg.analytics.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Recordatorio que un usuario configura sobre un evento.
 *
 * El scheduler del sistema evaluará cada minuto qué recordatorios
 * deben dispararse (sent=false AND event.eventDate - minutesBefore <= NOW).
 *
 * Restricción de unicidad: un usuario no puede crear dos recordatorios
 * para el mismo evento con los mismos minutos de antelación.
 */
@Entity
@Table(
    name = "reminders",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_reminders_user_event_minutes",
            columnNames = {"user_id", "event_id", "minutes_before"}
        )
    },
    indexes = {
        @Index(name = "idx_reminders_user_id",  columnList = "user_id"),
        @Index(name = "idx_reminders_event_id", columnList = "event_id"),
        @Index(name = "idx_reminders_pending",  columnList = "sent")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "minutesBefore", "sent"})
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Minutos de antelación al evento para enviar la notificación.
     * Valores esperados: 15, 30, 60, 1440 (1 día).
     */
    @Column(name = "minutes_before", nullable = false)
    private int minutesBefore;

    /** true cuando se envió el recordatorio X minutos antes del evento */
    @Column(name = "sent", nullable = false)
    @Builder.Default
    private boolean sent = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** true cuando se envió el email "hoy es el evento" (a las 08:00 del día del evento) */
    @Column(name = "day_reminder_sent", nullable = false)
    @Builder.Default
    private boolean dayReminderSent = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void markSent() {
        this.sent   = true;
        this.sentAt = LocalDateTime.now();
    }

    public void markDayReminderSent() {
        this.dayReminderSent = true;
    }
}
