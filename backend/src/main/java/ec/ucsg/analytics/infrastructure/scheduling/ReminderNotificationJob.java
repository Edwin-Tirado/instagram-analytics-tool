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
 *
 * Nota importante sobre LAZY loading:
 *   Las queries nativas devuelven entidades con relaciones LAZY. Se accede a
 *   reminder.getEvent().getImages() dentro de la transacción (@Transactional)
 *   para forzar la carga ANTES de pasar los objetos al método @Async del
 *   EmailNotificationService, que se ejecuta fuera de la sesión Hibernate.
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
                // Forzar carga LAZY de imágenes dentro de la transacción activa
                // antes de pasarlos al método @Async que corre fuera de la sesión.
                int imageCount = reminder.getEvent().getImages().size();
                log.debug("Imágenes pre-cargadas: {} para recordatorio {}", imageCount, reminder.getId());

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
                // Forzar carga LAZY de imágenes dentro de la transacción activa
                int imageCount = reminder.getEvent().getImages().size();
                log.debug("Imágenes pre-cargadas: {} para recordatorio {}", imageCount, reminder.getId());

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
