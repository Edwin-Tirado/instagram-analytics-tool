package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.infrastructure.mail.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

/**
 * Endpoint de diagnóstico SMTP — solo activo en perfil dev/default.
 * Llama a GET /api/test/mail?to=tu@email.com para verificar que
 * la conexión con Mailtrap funciona independientemente del flujo de negocio.
 *
 * ELIMINAR antes de producción (o mantener con @Profile("dev")).
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Profile("!prod")
public class MailTestController {

    private final JavaMailSender           mailSender;
    private final EmailNotificationService emailService;

    @GetMapping("/mail")
    public ResponseEntity<Map<String, String>> sendTestMail(
            @RequestParam(defaultValue = "test@ucsg.edu.ec") String to) {

        log.info(">>> TEST MAIL: enviando a {}", to);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@ucsg.edu.ec", "UCSG Eventos");
            helper.setTo(to);
            helper.setSubject("✅ Test SMTP — UCSG Eventos");
            helper.setText("""
                <h2 style="color:#931934">Conexión SMTP verificada</h2>
                <p>Si ves este email en Mailtrap, la configuración es correcta.</p>
                <p>Servidor: sandbox.smtp.mailtrap.io:2525</p>
                """, true);

            mailSender.send(message);
            log.info(">>> TEST MAIL: enviado exitosamente a {}", to);

            return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Email enviado a " + to + " — revisa Mailtrap"
            ));

        } catch (Exception e) {
            log.error(">>> TEST MAIL: FALLÓ — {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/mail/async")
    public ResponseEntity<Map<String, String>> sendAsyncTestMail(
            @RequestParam(defaultValue = "test@ucsg.edu.ec") String to) {

        log.info(">>> TEST MAIL ASYNC: disparando a {}", to);
        emailService.send(to, "✅ Test ASYNC — UCSG Eventos",
            "<h2 style='color:#931934'>@Async funciona correctamente</h2>" +
            "<p>Este email se envió desde el pool de hilos asíncrono de Spring.</p>");

        return ResponseEntity.ok(Map.of(
            "status", "dispatched",
            "message", "Email async disparado a " + to + " — revisa logs y Mailtrap en ~2s"
        ));
    }
}
