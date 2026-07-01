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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy 'a las' HH:mm", new Locale("es", "EC"));

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // ── 1. Confirmación inmediata al guardar el recordatorio ─────────────────

    @Async
    public void sendReminderConfirmation(AppUser user, Event event, Reminder reminder) {
        String label   = formatMinutes(reminder.getMinutesBefore());
        String subject = "✅ Recordatorio guardado: " + event.getTitle();
        String body    = buildConfirmationHtml(user, event, label);
        send(user.getEmail(), subject, body);
    }

    // ── 2. Email del día del evento (a las 08:00) ────────────────────────────

    @Async
    public void sendDayReminderEmail(AppUser user, Event event) {
        String subject = "📅 Hoy es el día: " + event.getTitle();
        String body    = buildDayReminderHtml(user, event);
        send(user.getEmail(), subject, body);
    }

    // ── 3. Recordatorio X minutos antes del evento ───────────────────────────

    @Async
    public void sendReminderEmail(AppUser user, Event event, Reminder reminder) {
        String subject = "⏰ Recordatorio: " + event.getTitle();
        String body    = buildReminderHtml(user, event, reminder);
        send(user.getEmail(), subject, body);
    }

    // ── Core de envío ────────────────────────────────────────────────────────

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

    // ── Templates HTML ────────────────────────────────────────────────────────

    private String buildConfirmationHtml(AppUser user, Event event, String label) {
        String dateStr   = event.getEventDate() != null
            ? event.getEventDate().format(DATE_FORMAT) : "Fecha por confirmar";
        String zone      = event.getZone() != null ? event.getZone().getName() : "Campus UCSG";
        String name      = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String imageHtml = buildImageBlock(event);
        String ctaUrl    = buildEventUrl(event);
        int    year      = java.time.Year.now().getValue();

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family:Arial,sans-serif;background:#f5f0eb;padding:20px;margin:0;">
              <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.08);">
                <div style="background:#931934;color:#fff;padding:28px 32px;">
                  <p style="margin:0 0 4px;font-size:13px;opacity:.8;letter-spacing:2px;text-transform:uppercase;">UCSG Eventos</p>
                  <h1 style="margin:0;font-size:22px;font-weight:700;">Recordatorio guardado ✅</h1>
                </div>
                %s
                <div style="padding:32px;">
                  <p style="color:#4a3728;margin:0 0 8px;">Hola, <strong>%s</strong></p>
                  <p style="color:#6b5344;margin:0 0 24px;">Tu recordatorio fue registrado exitosamente. Te avisaremos <strong>%s</strong> antes del evento, y también recibirás un aviso <strong>24 horas antes</strong> y otro <strong>15 minutos antes</strong> del inicio.</p>
                  <div style="background:#fdf6f0;border-left:4px solid #931934;padding:18px 20px;border-radius:6px;margin-bottom:28px;">
                    <h2 style="margin:0 0 10px;color:#931934;font-size:17px;">%s</h2>
                    <p style="margin:4px 0;color:#4a3728;">📅 <strong>%s</strong></p>
                    <p style="margin:4px 0;color:#4a3728;">📍 <strong>%s</strong></p>
                  </div>
                  <div style="text-align:center;margin-bottom:8px;">
                    <a href="%s" target="_blank"
                       style="display:inline-block;background:#931934;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:13px 32px;border-radius:8px;letter-spacing:.3px;">
                      🔔 Recordarme este evento
                    </a>
                  </div>
                  <p style="color:#9e8070;font-size:12px;text-align:center;margin:12px 0 0;">Si ya tienes un recordatorio configurado, puedes gestionarlo desde el sitio.</p>
                </div>
                <div style="background:#fdf6f0;padding:16px 32px;text-align:center;color:#b89f90;font-size:12px;">
                  © %d Universidad Católica de Santiago de Guayaquil
                </div>
              </div>
            </body>
            </html>
            """.formatted(imageHtml, name, label, event.getTitle(), dateStr, zone, ctaUrl, year);
    }

    private String buildDayReminderHtml(AppUser user, Event event) {
        String timeStr   = event.getEventDate() != null
            ? event.getEventDate().format(TIME_FORMAT) : "—";
        String zone      = event.getZone() != null ? event.getZone().getName() : "Campus UCSG";
        String name      = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String imageHtml = buildImageBlock(event);
        String ctaUrl    = buildEventUrl(event);
        int    year      = java.time.Year.now().getValue();

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family:Arial,sans-serif;background:#f5f0eb;padding:20px;margin:0;">
              <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.08);">
                <div style="background:#931934;color:#fff;padding:28px 32px;">
                  <p style="margin:0 0 4px;font-size:13px;opacity:.8;letter-spacing:2px;text-transform:uppercase;">UCSG Eventos</p>
                  <h1 style="margin:0;font-size:22px;font-weight:700;">¡Hoy es el día! 📅</h1>
                </div>
                %s
                <div style="padding:32px;">
                  <p style="color:#4a3728;margin:0 0 8px;">Hola, <strong>%s</strong></p>
                  <p style="color:#6b5344;margin:0 0 24px;">El evento que marcaste comienza <strong>hoy</strong>. No te lo pierdas.</p>
                  <div style="background:#fdf6f0;border-left:4px solid #931934;padding:18px 20px;border-radius:6px;margin-bottom:28px;">
                    <h2 style="margin:0 0 10px;color:#931934;font-size:17px;">%s</h2>
                    <p style="margin:4px 0;color:#4a3728;">🕐 <strong>Hora de inicio: %s</strong></p>
                    <p style="margin:4px 0;color:#4a3728;">📍 <strong>%s</strong></p>
                  </div>
                  <div style="text-align:center;margin-bottom:8px;">
                    <a href="%s" target="_blank"
                       style="display:inline-block;background:#931934;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:13px 32px;border-radius:8px;letter-spacing:.3px;">
                      🔔 Recordarme este evento
                    </a>
                  </div>
                  <p style="color:#9e8070;font-size:12px;text-align:center;margin:12px 0 0;">Recibirás un último recordatorio minutos antes de que inicie el evento.</p>
                </div>
                <div style="background:#fdf6f0;padding:16px 32px;text-align:center;color:#b89f90;font-size:12px;">
                  © %d Universidad Católica de Santiago de Guayaquil
                </div>
              </div>
            </body>
            </html>
            """.formatted(imageHtml, name, event.getTitle(), timeStr, zone, ctaUrl, year);
    }

    private String buildReminderHtml(AppUser user, Event event, Reminder reminder) {
        String dateStr   = event.getEventDate() != null
            ? event.getEventDate().format(DATE_FORMAT) : "Fecha por confirmar";
        String zone      = event.getZone() != null ? event.getZone().getName() : "Campus UCSG";
        String name      = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String label     = formatMinutes(reminder.getMinutesBefore());
        String imageHtml = buildImageBlock(event);
        String ctaUrl    = buildEventUrl(event);
        int    year      = java.time.Year.now().getValue();

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family:Arial,sans-serif;background:#f5f0eb;padding:20px;margin:0;">
              <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.08);">
                <div style="background:#931934;color:#fff;padding:28px 32px;">
                  <p style="margin:0 0 4px;font-size:13px;opacity:.8;letter-spacing:2px;text-transform:uppercase;">UCSG Eventos</p>
                  <h1 style="margin:0;font-size:22px;font-weight:700;">⏰ ¡El evento está por comenzar!</h1>
                </div>
                %s
                <div style="padding:32px;">
                  <p style="color:#4a3728;margin:0 0 8px;">Hola, <strong>%s</strong></p>
                  <p style="color:#6b5344;margin:0 0 24px;">Faltan <strong>%s</strong> para que inicie el evento.</p>
                  <div style="background:#fdf6f0;border-left:4px solid #931934;padding:18px 20px;border-radius:6px;margin-bottom:28px;">
                    <h2 style="margin:0 0 10px;color:#931934;font-size:17px;">%s</h2>
                    <p style="margin:4px 0;color:#4a3728;">📅 <strong>%s</strong></p>
                    <p style="margin:4px 0;color:#4a3728;">📍 <strong>%s</strong></p>
                  </div>
                  <div style="text-align:center;margin-bottom:8px;">
                    <a href="%s" target="_blank"
                       style="display:inline-block;background:#931934;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:13px 32px;border-radius:8px;letter-spacing:.3px;">
                      🔔 Recordarme este evento
                    </a>
                  </div>
                  <p style="color:#9e8070;font-size:12px;text-align:center;margin:12px 0 0;">Este fue el último recordatorio configurado para este evento. ¡Que lo disfrutes!</p>
                </div>
                <div style="background:#fdf6f0;padding:16px 32px;text-align:center;color:#b89f90;font-size:12px;">
                  © %d Universidad Católica de Santiago de Guayaquil
                </div>
              </div>
            </body>
            </html>
            """.formatted(imageHtml, name, label, event.getTitle(), dateStr, zone, ctaUrl, year);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Construye el bloque HTML de imagen responsiva del evento.
     * Usa la primera imagen del carrusel; si no hay imágenes, devuelve bloque vacío.
     */
    private String buildImageBlock(Event event) {
        if (event.getImages() == null || event.getImages().isEmpty()) {
            return "";
        }
        String imageUrl = event.getImages().get(0).getMediaUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        return """
            <div style="text-align:center;background:#fdf6f0;padding:0;">
              <img src="%s"
                   alt="%s"
                   width="600"
                   style="display:block;width:100%%;max-width:600px;height:auto;object-fit:cover;max-height:320px;" />
            </div>
            """.formatted(imageUrl, escapeHtml(event.getTitle()));
    }

    /**
     * Construye la URL de deep-link al evento en el frontend.
     * Formato: {frontendUrl}/events?id={eventId}
     */
    private String buildEventUrl(Event event) {
        return frontendUrl + "/events?id=" + event.getId();
    }

    /** Escapa caracteres HTML en atributos de imagen. */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private String formatMinutes(int minutes) {
        if (minutes < 60)   return minutes + " minutos";
        if (minutes == 60)  return "1 hora";
        if (minutes < 1440) return (minutes / 60) + " horas";
        if (minutes == 1440) return "1 día";
        return (minutes / 1440) + " días";
    }
}
