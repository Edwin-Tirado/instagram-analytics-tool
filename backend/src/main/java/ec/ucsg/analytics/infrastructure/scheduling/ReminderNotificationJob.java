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
 *   2. 3 días antes → fijo, para todos los recordatorios (processThreeDaysBeforeReminders)
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
                // Forzar carga LAZY de imágenes y zona dentro de la transacción activa
                // antes de pasarlos al método @Async que corre fuera de la sesión.
                int imageCount = reminder.getEvent().getImages().size();
                if (reminder.getEvent().getZone() != null) reminder.getEvent().getZone().getName();
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

    // ── Email fijo de 3 días antes ───────────────────────────────────────────

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processThreeDaysBeforeReminders() {
        List<Reminder> due = reminderRepository.findThreeDaysBeforeReminders(LocalDateTime.now());
        if (due.isEmpty()) return;

        log.info("Procesando {} recordatorios de 3 días antes", due.size());

        for (Reminder reminder : due) {
            try {
                // Forzar carga LAZY de imágenes y zona dentro de la transacción activa
                int imageCount = reminder.getEvent().getImages().size();
                if (reminder.getEvent().getZone() != null) reminder.getEvent().getZone().getName();
                log.debug("Imágenes pre-cargadas: {} para recordatorio {}", imageCount, reminder.getId());

                emailService.sendThreeDaysBeforeEmail(
                    reminder.getUser(),
                    reminder.getEvent()
                );
                reminder.markThreeDaysReminderSent();
                reminderRepository.save(reminder);
                log.debug("Email de 3 días antes enviado para recordatorio {}", reminder.getId());
            } catch (Exception e) {
                log.error("Error enviando email de 3 días antes {} — se reintentará: {}", reminder.getId(), e.getMessage());
            }
        }
    }
}
