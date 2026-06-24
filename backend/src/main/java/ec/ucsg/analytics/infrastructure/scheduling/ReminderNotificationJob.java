package ec.ucsg.analytics.infrastructure.scheduling;

import ec.ucsg.analytics.domain.model.Reminder;
import ec.ucsg.analytics.domain.repository.ReminderRepository;
import ec.ucsg.analytics.infrastructure.mail.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job que evalúa y dispara los recordatorios pendientes.
 *
 * Se ejecuta cada minuto. Para cada {@link Reminder} cuya ventana ya venció
 * (event_date - minutes_before <= ahora), envía el email y marca sent=true.
 *
 * Si el envío de email falla el recordatorio se deja sin marcar
 * para que el siguiente tick lo reintente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderNotificationJob {

    private final ReminderRepository     reminderRepository;
    private final EmailNotificationService emailService;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processReminders() {
        List<Reminder> due = reminderRepository.findDueReminders(LocalDateTime.now());

        if (due.isEmpty()) return;

        log.info("Procesando {} recordatorios pendientes", due.size());

        for (Reminder reminder : due) {
            try {
                emailService.sendReminderEmail(
                    reminder.getUser(),
                    reminder.getEvent(),
                    reminder
                );
                reminder.markSent();
                reminderRepository.save(reminder);
                log.debug("Recordatorio {} marcado como enviado", reminder.getId());

            } catch (Exception e) {
                log.error("Error enviando recordatorio {} — se reintentará: {}",
                    reminder.getId(), e.getMessage());
            }
        }
    }
}
