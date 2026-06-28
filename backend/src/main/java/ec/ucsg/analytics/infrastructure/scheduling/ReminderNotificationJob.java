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
 * Job que evalúa y dispara los recordatorios pendientes cada minuto.
 *
 * Flujo de emails por recordatorio:
 *   1. Confirmación → inmediata al crear el recordatorio (ReminderService)
 *   2. Día del evento → a las 08:00 del día del evento (processDayReminders)
 *   3. X minutos antes → según minutesBefore configurado (processDueReminders)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderNotificationJob {

    private final ReminderRepository       reminderRepository;
    private final EmailNotificationService emailService;

    // ── Recordatorio X minutos antes ────────────────────────────────────────

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processDueReminders() {
        List<Reminder> due = reminderRepository.findDueReminders(LocalDateTime.now());
        if (due.isEmpty()) return;

        log.info("Procesando {} recordatorios de cuenta regresiva", due.size());

        for (Reminder reminder : due) {
            try {
                emailService.sendReminderEmail(
                    reminder.getUser(),
                    reminder.getEvent(),
                    reminder
                );
                reminder.markSent();
                reminderRepository.save(reminder);
                log.debug("Recordatorio {} ({}min antes) marcado como enviado", reminder.getId(), reminder.getMinutesBefore());
            } catch (Exception e) {
                log.error("Error enviando recordatorio {} — se reintentará: {}", reminder.getId(), e.getMessage());
            }
        }
    }

    // ── Email del día del evento (08:00) ────────────────────────────────────

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processDayReminders() {
        List<Reminder> due = reminderRepository.findDayReminders(LocalDateTime.now());
        if (due.isEmpty()) return;

        log.info("Procesando {} recordatorios de día del evento", due.size());

        for (Reminder reminder : due) {
            try {
                emailService.sendDayReminderEmail(
                    reminder.getUser(),
                    reminder.getEvent()
                );
                reminder.markDayReminderSent();
                reminderRepository.save(reminder);
                log.debug("Email día-del-evento enviado para recordatorio {}", reminder.getId());
            } catch (Exception e) {
                log.error("Error enviando email día-del-evento {} — se reintentará: {}", reminder.getId(), e.getMessage());
            }
        }
    }
}
