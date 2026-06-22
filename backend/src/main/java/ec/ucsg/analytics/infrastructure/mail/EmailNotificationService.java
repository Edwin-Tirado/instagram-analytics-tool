package ec.ucsg.analytics.infrastructure.mail;

import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.model.Event;
import ec.ucsg.analytics.domain.model.Reminder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Servicio de notificaciones por correo electrónico.
 *
 * Configurado para Mailtrap (sandbox SMTP) en desarrollo.
 * Cambiar las variables de entorno MAILTRAP_* en producción por
 * las credenciales del servidor SMTP real de UCSG.
 *
 * Todos los envíos son {@code @Async} para no bloquear el hilo
 * del scheduler o del request HTTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy 'a las' HH:mm", new Locale("es", "EC"));

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    // ── Recordatorio de evento ───────────────────────────────────────

    /**
     * Envía un recordatorio al usuario antes de que inicie el evento.
     * Disparado por el scheduler de recordatorios (ReminderJob — Sprint 3).
     */
    @Async
    public void sendReminderEmail(AppUser user, Event event, Reminder reminder) {
        String subject = "⏰ Recordatorio: " + event.getTitle();
        String body    = buildReminderHtml(user, event, reminder);
        send(user.getEmail(), subject, body);
    }

    // ── Notificación de aprobación ───────────────────────────────────

    /**
     * Notifica a todos los usuarios con recordatorios activos que el evento
     * fue aprobado y está disponible en el mapa.
     * Disparado por {@code EventService.approveEvent()}.
     */
    @Async
    public void sendApprovalNotification(AppUser user, Event event) {
        String subject = "✅ Evento aprobado: " + event.getTitle();
        String body    = buildApprovalHtml(user, event);
        send(user.getEmail(), subject, body);
    }

    // ── Core de envío ────────────────────────────────────────────────

    @Async
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Correo enviado a {}: {}", to, subject);

        } catch (MessagingException e) {
            log.error("Error enviando correo a {}: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en envío de correo: {}", e.getMessage());
        }
    }

    // ── Templates HTML ───────────────────────────────────────────────

    private String buildReminderHtml(AppUser user, Event event, Reminder reminder) {
        String dateStr = event.getEventDate() != null
            ? event.getEventDate().format(DATE_FORMAT)
            : "Fecha por confirmar";

        String zone = event.getZone() != null ? event.getZone().getName() : "Campus UCSG";

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;">
              <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;">
                <div style="background:#003087;color:#fff;padding:24px 32px;">
                  <h1 style="margin:0;font-size:22px;">UCSG Campus Analytics</h1>
                  <p style="margin:4px 0 0;opacity:.85;">Sistema de Eventos Universitarios</p>
                </div>
                <div style="padding:32px;">
                  <p style="color:#555;">Hola, <strong>%s</strong></p>
                  <p>Te recordamos que el siguiente evento comienza en <strong>%d minutos</strong>:</p>
                  <div style="background:#f0f4ff;border-left:4px solid #003087;padding:16px;border-radius:4px;margin:16px 0;">
                    <h2 style="margin:0 0 8px;color:#003087;">%s</h2>
                    <p style="margin:4px 0;">📅 <strong>%s</strong></p>
                    <p style="margin:4px 0;">📍 <strong>%s</strong></p>
                  </div>
                  <p style="color:#888;font-size:13px;">Este recordatorio fue configurado por ti desde el mapa de eventos.</p>
                </div>
                <div style="background:#f9f9f9;padding:16px 32px;text-align:center;color:#aaa;font-size:12px;">
                  © %d Universidad Católica de Santiago de Guayaquil
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                user.getFullName() != null ? user.getFullName() : user.getEmail(),
                reminder.getMinutesBefore(),
                event.getTitle(),
                dateStr,
                zone,
                java.time.Year.now().getValue()
            );
    }

    private String buildApprovalHtml(AppUser user, Event event) {
        String dateStr = event.getEventDate() != null
            ? event.getEventDate().format(DATE_FORMAT)
            : "Fecha por confirmar";

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;">
              <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;">
                <div style="background:#003087;color:#fff;padding:24px 32px;">
                  <h1 style="margin:0;font-size:22px;">UCSG Campus Analytics</h1>
                </div>
                <div style="padding:32px;">
                  <p>Hola, <strong>%s</strong></p>
                  <p>Un evento que podrías interesarte ya está disponible en el mapa:</p>
                  <div style="background:#f0fff4;border-left:4px solid #28a745;padding:16px;border-radius:4px;margin:16px 0;">
                    <h2 style="margin:0 0 8px;color:#155724;">%s</h2>
                    <p style="margin:4px 0;">📅 <strong>%s</strong></p>
                  </div>
                  <p>Consulta el mapa del campus para ver la ubicación exacta y configurar un recordatorio.</p>
                </div>
                <div style="background:#f9f9f9;padding:16px 32px;text-align:center;color:#aaa;font-size:12px;">
                  © %d Universidad Católica de Santiago de Guayaquil
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                user.getFullName() != null ? user.getFullName() : user.getEmail(),
                event.getTitle(),
                dateStr,
                java.time.Year.now().getValue()
            );
    }
}
