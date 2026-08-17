package ec.ucsg.analytics.infrastructure.mail;

import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.model.Event;
import ec.ucsg.analytics.domain.model.EventImage;
import ec.ucsg.analytics.domain.model.Reminder;
import ec.ucsg.analytics.infrastructure.instagram.InstagramImageUrlRefresher;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy 'a las' HH:mm", new Locale("es", "EC"));

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm");

    /** Content-ID de la imagen embebida — debe coincidir entre el HTML (cid:) y el adjunto inline. */
    private static final String INLINE_IMAGE_CID = "eventImage";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private final JavaMailSender               mailSender;
    private final InstagramImageUrlRefresher    imageUrlRefresher;

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
        Optional<InlineImage> image = loadInlineImage(event);
        String subject = "✅ Recordatorio guardado: " + event.getTitle();
        String body    = buildConfirmationHtml(user, event, label, image.isPresent());
        send(user.getEmail(), subject, body, image.orElse(null));
    }

    // ── 2. Email del día del evento (a las 08:00) ────────────────────────────

    @Async
    public void sendDayReminderEmail(AppUser user, Event event) {
        Optional<InlineImage> image = loadInlineImage(event);
        String subject = "📅 Hoy es el día: " + event.getTitle();
        String body    = buildDayReminderHtml(user, event, image.isPresent());
        send(user.getEmail(), subject, body, image.orElse(null));
    }

    // ── 3. Recordatorio X minutos antes del evento ───────────────────────────

    @Async
    public void sendReminderEmail(AppUser user, Event event, Reminder reminder) {
        Optional<InlineImage> image = loadInlineImage(event);
        String subject = "⏰ Recordatorio: " + event.getTitle();
        String body    = buildReminderHtml(user, event, reminder, image.isPresent());
        send(user.getEmail(), subject, body, image.orElse(null));
    }

    // ── 4. Nuevo evento aprobado en una zona de interés del usuario ──────────

    @Async
    public void sendNewEventNotification(AppUser user, Event event) {
        Optional<InlineImage> image = loadInlineImage(event);
        String subject = "🎉 Nuevo evento en tu zona de interés: " + event.getTitle();
        String body    = buildNewEventHtml(user, event, image.isPresent());
        send(user.getEmail(), subject, body, image.orElse(null));
    }

    // ── 5. Evento editado — notificar a usuarios con recordatorio ────────────

    @Async
    public void sendEventEditedNotification(AppUser user, Event event) {
        Optional<InlineImage> image = loadInlineImage(event);
        String subject = "✏️ Evento actualizado: " + event.getTitle();
        String body    = buildEventEditedHtml(user, event, image.isPresent());
        send(user.getEmail(), subject, body, image.orElse(null));
    }

    // ── 6. Evento eliminado — notificar a usuarios con recordatorio ──────────

    @Async
    public void sendEventDeletedNotification(AppUser user, String eventTitle) {
        String subject = "❌ Evento cancelado: " + eventTitle;
        String body    = buildEventDeletedHtml(user, eventTitle);
        send(user.getEmail(), subject, body);
    }

    /**
     * Genera el HTML de un ícono pequeño embebido (ver {@link EmailIcons}) para
     * usar dentro de las plantillas, en vez de un emoji directo en el texto —
     * evita el "tofu" (cuadro vacío) que a veces renderiza el emoji según la
     * fuente del cliente de correo.
     */
    private static String icon(String dataUri, int size) {
        return "<img src=\"%s\" width=\"%d\" height=\"%d\" alt=\"\" style=\"vertical-align:middle;display:inline-block;\" />"
            .formatted(dataUri, size, size);
    }

    // ── Core de envío ────────────────────────────────────────────────────────

    @Async
    public void send(String to, String subject, String htmlBody) {
        send(to, subject, htmlBody, null);
    }

    private void send(String to, String subject, String htmlBody, InlineImage image) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (image != null) {
                helper.addInline(INLINE_IMAGE_CID, new ByteArrayResource(image.bytes()), image.contentType());
            }
            mailSender.send(message);
            log.info("Correo enviado a {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Error enviando correo a {}: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en envío de correo: {}", e.getMessage());
        }
    }

    // ── Templates HTML ────────────────────────────────────────────────────────

    private String buildConfirmationHtml(AppUser user, Event event, String label, boolean hasImage) {
        String dateStr   = event.getEventDate() != null
            ? event.getEventDate().format(DATE_FORMAT) : "Fecha por confirmar";
        String zone      = event.getZone() != null ? event.getZone().getName() : "Campus UCSG";
        String name      = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String imageHtml = buildImageBlock(hasImage, event.getTitle());
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
                  <h1 style="margin:0;font-size:22px;font-weight:700;">Recordatorio guardado %s</h1>
                </div>
                %s
                <div style="padding:32px;">
                  <p style="color:#4a3728;margin:0 0 8px;">Hola, <strong>%s</strong></p>
                  <p style="color:#6b5344;margin:0 0 24px;">Tu recordatorio fue registrado exitosamente. Te avisaremos <strong>%s</strong> antes del evento, y también recibirás un aviso la mañana del evento (8:00 a.m.) para que no se te pase.</p>
                  <div style="background:#fdf6f0;border-left:4px solid #931934;padding:18px 20px;border-radius:6px;margin-bottom:28px;">
                    <h2 style="margin:0 0 10px;color:#931934;font-size:17px;">%s</h2>
                    <p style="margin:4px 0;color:#4a3728;">%s <strong>%s</strong></p>
                    <p style="margin:4px 0;color:#4a3728;">%s <strong>%s</strong></p>
                  </div>
                  <div style="text-align:center;margin-bottom:8px;">
                    <a href="%s" target="_blank"
                       style="display:inline-block;background:#931934;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:13px 32px;border-radius:8px;letter-spacing:.3px;">
                      %s Recordarme este evento
                    </a>
                  </div>
                  <p style="color:#9e8070;font-size:12px;text-align:center;margin:12px 0 0;">Si ya tienes un recordatorio configurado, puedes gestionarlo desde el sitio.</p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(
                icon(EmailIcons.CHECK_WHITE, 20), imageHtml, name, label, event.getTitle(),
                icon(EmailIcons.CALENDAR_CRIMSON, 15), dateStr,
                icon(EmailIcons.MAPPIN_CRIMSON, 15), zone,
                ctaUrl, icon(EmailIcons.BELL_WHITE, 16), buildContactFooterHtml(year));
    }

    private String buildDayReminderHtml(AppUser user, Event event, boolean hasImage) {
        String timeStr   = event.getEventDate() != null
            ? event.getEventDate().format(TIME_FORMAT) : "—";
        String zone      = event.getZone() != null ? event.getZone().getName() : "Campus UCSG";
        String name      = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String imageHtml = buildImageBlock(hasImage, event.getTitle());
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
                  <h1 style="margin:0;font-size:22px;font-weight:700;">¡Hoy es el día! %s</h1>
                </div>
                %s
                <div style="padding:32px;">
                  <p style="color:#4a3728;margin:0 0 8px;">Hola, <strong>%s</strong></p>
                  <p style="color:#6b5344;margin:0 0 24px;">El evento que marcaste comienza <strong>hoy</strong>. No te lo pierdas.</p>
                  <div style="background:#fdf6f0;border-left:4px solid #931934;padding:18px 20px;border-radius:6px;margin-bottom:28px;">
                    <h2 style="margin:0 0 10px;color:#931934;font-size:17px;">%s</h2>
                    <p style="margin:4px 0;color:#4a3728;">%s <strong>Hora de inicio: %s</strong></p>
                    <p style="margin:4px 0;color:#4a3728;">%s <strong>%s</strong></p>
                  </div>
                  <div style="text-align:center;margin-bottom:8px;">
                    <a href="%s" target="_blank"
                       style="display:inline-block;background:#931934;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:13px 32px;border-radius:8px;letter-spacing:.3px;">
                      %s Recordarme este evento
                    </a>
                  </div>
                  <p style="color:#9e8070;font-size:12px;text-align:center;margin:12px 0 0;">Recibirás un último recordatorio minutos antes de que inicie el evento.</p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(
                icon(EmailIcons.CALENDAR_WHITE, 20), imageHtml, name, event.getTitle(),
                icon(EmailIcons.CLOCK_CRIMSON, 15), timeStr,
                icon(EmailIcons.MAPPIN_CRIMSON, 15), zone,
                ctaUrl, icon(EmailIcons.BELL_WHITE, 16), buildContactFooterHtml(year));
    }

    private String buildReminderHtml(AppUser user, Event event, Reminder reminder, boolean hasImage) {
        String dateStr   = event.getEventDate() != null
            ? event.getEventDate().format(DATE_FORMAT) : "Fecha por confirmar";
        String zone      = event.getZone() != null ? event.getZone().getName() : "Campus UCSG";
        String name      = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String label     = formatMinutes(reminder.getMinutesBefore());
        String imageHtml = buildImageBlock(hasImage, event.getTitle());
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
                  <h1 style="margin:0;font-size:22px;font-weight:700;">%s ¡El evento está por comenzar!</h1>
                </div>
                %s
                <div style="padding:32px;">
                  <p style="color:#4a3728;margin:0 0 8px;">Hola, <strong>%s</strong></p>
                  <p style="color:#6b5344;margin:0 0 24px;">Faltan <strong>%s</strong> para que inicie el evento.</p>
                  <div style="background:#fdf6f0;border-left:4px solid #931934;padding:18px 20px;border-radius:6px;margin-bottom:28px;">
                    <h2 style="margin:0 0 10px;color:#931934;font-size:17px;">%s</h2>
                    <p style="margin:4px 0;color:#4a3728;">%s <strong>%s</strong></p>
                    <p style="margin:4px 0;color:#4a3728;">%s <strong>%s</strong></p>
                  </div>
                  <div style="text-align:center;margin-bottom:8px;">
                    <a href="%s" target="_blank"
                       style="display:inline-block;background:#931934;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:13px 32px;border-radius:8px;letter-spacing:.3px;">
                      %s Recordarme este evento
                    </a>
                  </div>
                  <p style="color:#9e8070;font-size:12px;text-align:center;margin:12px 0 0;">Este fue el último recordatorio configurado para este evento. ¡Que lo disfrutes!</p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(
                icon(EmailIcons.CLOCK_WHITE, 20), imageHtml, name, label, event.getTitle(),
                icon(EmailIcons.CALENDAR_CRIMSON, 15), dateStr,
                icon(EmailIcons.MAPPIN_CRIMSON, 15), zone,
                ctaUrl, icon(EmailIcons.BELL_WHITE, 16), buildContactFooterHtml(year));
    }

    private String buildNewEventHtml(AppUser user, Event event, boolean hasImage) {
        String dateStr   = event.getEventDate() != null
            ? event.getEventDate().format(DATE_FORMAT) : "Fecha por confirmar";
        String zone      = event.getZone() != null ? event.getZone().getName() : "Campus UCSG";
        String name      = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String imageHtml = buildImageBlock(hasImage, event.getTitle());
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
                  <h1 style="margin:0;font-size:22px;font-weight:700;">Nuevo evento en tu zona %s</h1>
                </div>
                %s
                <div style="padding:32px;">
                  <p style="color:#4a3728;margin:0 0 8px;">Hola, <strong>%s</strong></p>
                  <p style="color:#6b5344;margin:0 0 24px;">Se acaba de aprobar un evento en <strong>%s</strong>, una zona donde ya has puesto recordatorios antes. Puede interesarte:</p>
                  <div style="background:#fdf6f0;border-left:4px solid #931934;padding:18px 20px;border-radius:6px;margin-bottom:28px;">
                    <h2 style="margin:0 0 10px;color:#931934;font-size:17px;">%s</h2>
                    <p style="margin:4px 0;color:#4a3728;">%s <strong>%s</strong></p>
                    <p style="margin:4px 0;color:#4a3728;">%s <strong>%s</strong></p>
                  </div>
                  <div style="text-align:center;margin-bottom:8px;">
                    <a href="%s" target="_blank"
                       style="display:inline-block;background:#931934;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:13px 32px;border-radius:8px;letter-spacing:.3px;">
                      %s Recordarme este evento
                    </a>
                  </div>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(
                icon(EmailIcons.PARTY_WHITE, 20), imageHtml, name, zone, event.getTitle(),
                icon(EmailIcons.CALENDAR_CRIMSON, 15), dateStr,
                icon(EmailIcons.MAPPIN_CRIMSON, 15), zone,
                ctaUrl, icon(EmailIcons.BELL_WHITE, 16), buildContactFooterHtml(year));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Descarga la primera imagen del evento y la deja lista para adjuntarse
     * inline (Content-ID) al MimeMessage.
     *
     * La media_url de Instagram almacenada en BD expira ~7 días después de la
     * ingesta — un correo puede dispararse mucho después (recordatorio del día
     * del evento, X minutos antes, etc.), así que en vez de enlazar directo a
     * esa URL (que puede estar muerta y romper la imagen en el cliente de
     * correo) se descarga el binario y se embebe en el propio email. Si la
     * descarga falla, se pide una URL fresca una sola vez antes de renunciar.
     */
    private Optional<InlineImage> loadInlineImage(Event event) {
        if (event.getImages() == null || event.getImages().isEmpty()) {
            return Optional.empty();
        }

        EventImage img = event.getImages().get(0);
        if (img.getMediaUrl() == null || img.getMediaUrl().isBlank()) {
            return Optional.empty();
        }

        Optional<InlineImage> result = tryDownloadImage(img.getMediaUrl());

        if (result.isEmpty() && img.getId() != null) {
            result = imageUrlRefresher.refreshSingleImageUrl(img.getId())
                .flatMap(this::tryDownloadImage);
        }

        if (result.isEmpty()) {
            log.warn("No se pudo adjuntar la imagen del evento '{}' al correo (URL de Instagram expirada o inaccesible)",
                event.getTitle());
        }

        return result;
    }

    private Optional<InlineImage> tryDownloadImage(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers().firstValue("Content-Type").orElse("image/jpeg");

            if (response.statusCode() == 200 && response.body().length > 0 && contentType.startsWith("image/")) {
                return Optional.of(new InlineImage(response.body(), contentType));
            }
            log.debug("Descarga de imagen para email respondió {} (content-type={})", response.statusCode(), contentType);
            return Optional.empty();
        } catch (Exception e) {
            log.debug("No se pudo descargar la imagen para el email: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Bytes de la imagen ya descargados, listos para adjuntarse inline (cid:eventImage). */
    private record InlineImage(byte[] bytes, String contentType) {}

    /**
     * Construye el bloque HTML de imagen responsiva del evento.
     * Referencia el adjunto inline (cid:) — nunca la URL remota de Instagram,
     * que puede haber expirado para cuando el destinatario abre el correo.
     */
    private String buildImageBlock(boolean hasImage, String altText) {
        if (!hasImage) {
            return "";
        }
        return """
            <div style="text-align:center;background:#fdf6f0;padding:0;">
              <img src="cid:%s"
                   alt="%s"
                   width="600"
                   style="display:block;width:100%%;max-width:600px;height:auto;object-fit:cover;max-height:320px;" />
            </div>
            """.formatted(INLINE_IMAGE_CID, escapeHtml(altText));
    }

    /**
     * Construye la URL de deep-link al evento en el frontend.
     * Formato: {frontendUrl}/events/{eventId} — página de detalle dedicada.
     */
    private String buildEventUrl(Event event) {
        return frontendUrl + "/events/" + event.getId();
    }

    /** Pie de página con los datos de contacto institucionales, común a todas las plantillas. */
    private String buildContactFooterHtml(int year) {
        return """
            <div style="background:#fdf6f0;padding:24px 32px;text-align:center;color:#6b5344;font-size:12px;border-top:1px solid #eaded3;">
              <p style="margin:0 0 10px;color:#931934;font-weight:700;font-size:13px;letter-spacing:1px;">CONTÁCTENOS</p>
              <p style="margin:2px 0;">Teléfono: +593 4 3804600 &nbsp;·&nbsp; WhatsApp: <a href="https://wa.me/593990994445" style="color:#6b5344;text-decoration:none;">0990994445</a></p>
              <p style="margin:2px 0;">Av. Carlos Julio Arosemena Km 1 ½</p>
              <p style="margin:2px 0;"><a href="mailto:admisiones@cu.ucsg.edu.ec" style="color:#6b5344;text-decoration:none;">admisiones@cu.ucsg.edu.ec</a> &nbsp;·&nbsp; <a href="mailto:info@cu.ucsg.edu.ec" style="color:#6b5344;text-decoration:none;">info@cu.ucsg.edu.ec</a></p>
              <p style="margin:10px 0 0;font-weight:600;color:#931934;">Sala de Prensa UCSG &nbsp;·&nbsp; <a href="mailto:info@cu.ucsg.edu.ec" style="color:#931934;text-decoration:none;">Envíenos su mensaje</a></p>
              <p style="margin:16px 0 0;color:#b89f90;">© %d Universidad Católica de Santiago de Guayaquil</p>
            </div>
            """.formatted(year);
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

    private String buildEventEditedHtml(AppUser user, Event event, boolean hasImage) {
        String dateStr   = event.getEventDate() != null
            ? event.getEventDate().format(DATE_FORMAT) : "Fecha por confirmar";
        String zone      = event.getZone() != null ? event.getZone().getName() : "Campus UCSG";
        String name      = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String imageHtml = buildImageBlock(hasImage, event.getTitle());
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
                  <h1 style="margin:0;font-size:22px;font-weight:700;">Evento actualizado %s</h1>
                </div>
                %s
                <div style="padding:32px;">
                  <p style="color:#4a3728;margin:0 0 8px;">Hola, <strong>%s</strong></p>
                  <p style="color:#6b5344;margin:0 0 24px;">Un evento en el que tienes un recordatorio ha sido <strong>modificado</strong>. Aquí están los datos actualizados:</p>
                  <div style="background:#fdf6f0;border-left:4px solid #931934;padding:18px 20px;border-radius:6px;margin-bottom:28px;">
                    <h2 style="margin:0 0 10px;color:#931934;font-size:17px;">%s</h2>
                    <p style="margin:4px 0;color:#4a3728;">%s <strong>%s</strong></p>
                    <p style="margin:4px 0;color:#4a3728;">%s <strong>%s</strong></p>
                  </div>
                  <div style="text-align:center;margin-bottom:8px;">
                    <a href="%s" target="_blank"
                       style="display:inline-block;background:#931934;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:13px 32px;border-radius:8px;letter-spacing:.3px;">
                      Ver evento actualizado
                    </a>
                  </div>
                  <p style="color:#9e8070;font-size:12px;text-align:center;margin:12px 0 0;">Si los nuevos datos no te convienen, puedes cancelar tu recordatorio desde el sitio.</p>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(
                icon(EmailIcons.PENCIL_WHITE, 18), imageHtml, name, event.getTitle(),
                icon(EmailIcons.CALENDAR_CRIMSON, 15), dateStr,
                icon(EmailIcons.MAPPIN_CRIMSON, 15), zone,
                ctaUrl, buildContactFooterHtml(year));
    }

    private String buildEventDeletedHtml(AppUser user, String eventTitle) {
        String name = user.getFullName() != null ? user.getFullName() : user.getEmail();
        int    year = java.time.Year.now().getValue();

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
                  <h1 style="margin:0;font-size:22px;font-weight:700;">Evento cancelado %s</h1>
                </div>
                <div style="padding:32px;">
                  <p style="color:#4a3728;margin:0 0 8px;">Hola, <strong>%s</strong></p>
                  <p style="color:#6b5344;margin:0 0 24px;">Lamentamos informarte que el siguiente evento al que tenías un recordatorio ha sido <strong>cancelado o eliminado</strong>:</p>
                  <div style="background:#fff5f5;border-left:4px solid #931934;padding:18px 20px;border-radius:6px;margin-bottom:28px;">
                    <h2 style="margin:0;color:#931934;font-size:17px;">%s</h2>
                  </div>
                  <p style="color:#6b5344;text-align:center;margin:0 0 24px;">Tu recordatorio ha sido cancelado automáticamente. Puedes explorar otros eventos disponibles en el portal.</p>
                  <div style="text-align:center;margin-bottom:8px;">
                    <a href="%s" target="_blank"
                       style="display:inline-block;background:#931934;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:13px 32px;border-radius:8px;letter-spacing:.3px;">
                      Ver otros eventos
                    </a>
                  </div>
                </div>
                %s
              </div>
            </body>
            </html>
            """.formatted(icon(EmailIcons.XCIRCLE_WHITE, 20), name, eventTitle, frontendUrl, buildContactFooterHtml(year));
    }
}
