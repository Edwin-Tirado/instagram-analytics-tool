package ec.ucsg.analytics.application.service;

import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.model.Event;
import ec.ucsg.analytics.domain.repository.ReminderRepository;
import ec.ucsg.analytics.infrastructure.mail.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Notifica por correo a los estudiantes "interesados" cuando se aprueba un evento nuevo.
 *
 * "Interesado" se define, con los datos que existen hoy, como: cualquier usuario que
 * ya haya puesto un recordatorio en OTRO evento de la misma zona (historial de
 * recordatorios) — no existe una tabla de preferencias/suscripciones dedicada.
 *
 * Corre en un bean separado de {@link EventService} a propósito: el proxy de
 * Spring que activa {@code @Async} solo intercepta llamadas entre beans distintos,
 * así que esto NUNCA debe fusionarse dentro de EventService (self-invocation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventApprovalNotifier {

    private final ReminderRepository       reminderRepository;
    private final EmailNotificationService emailService;

    /**
     * Punto de entrada llamado tras aprobar un evento. Corre en segundo plano y
     * nunca propaga una excepción — un fallo aquí no debe revertir ni bloquear
     * la aprobación, que ya quedó confirmada en la base de datos antes de esta llamada.
     */
    @Async
    public void notifyEventApproved(Event event) {
        if (event.getZone() == null) {
            log.debug("Evento '{}' sin zona asignada — no hay a quién notificar por zona", event.getTitle());
            return;
        }

        try {
            List<AppUser> interested = reminderRepository
                .findDistinctInterestedUsersByZoneId(event.getZone().getId(), event.getId());

            log.info("Notificando evento aprobado '{}' a {} usuario(s) interesado(s) en la zona '{}'",
                event.getTitle(), interested.size(), event.getZone().getName());

            for (AppUser user : interested) {
                try {
                    emailService.sendNewEventNotification(user, event);
                } catch (Exception e) {
                    // Resiliencia: el fallo de UN destinatario no debe frenar al resto.
                    log.error("Fallo al notificar el evento '{}' a {}: {}",
                        event.getTitle(), user.getEmail(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            // Resiliencia: cualquier fallo aquí (ej. error de consulta) queda en log,
            // el evento ya fue aprobado y persistido antes de llegar a este punto.
            log.error("Fallo notificando el evento aprobado '{}': {}", event.getTitle(), e.getMessage(), e);
        }
    }
}
