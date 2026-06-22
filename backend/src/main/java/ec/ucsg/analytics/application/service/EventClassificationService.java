package ec.ucsg.analytics.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Set;

/**
 * Clasificador heurístico de captions de Instagram.
 *
 * Reglas (se evalúan en orden):
 *   1. Si el caption contiene ≥ SPAM_THRESHOLD palabras de {@link #SPAM_KEYWORDS}
 *      → AUTO_REJECTED (publicidad/spam).
 *   2. Si contiene ≥ 1 palabra de {@link #EVENT_KEYWORDS}
 *      → EVENT (pasa a ingesta como PENDING).
 *   3. En otro caso → SKIP (no ingestar).
 *
 * Lista de keywords basada en el vocabulario habitual de @ucsgnotificaciones
 * y los nombres de zonas del mapa de referencia del campus UCSG.
 */
@Slf4j
@Service
public class EventClassificationService {

    private static final int SPAM_THRESHOLD = 1;

    // ── Palabras clave de SPAM / Publicidad ─────────────────────────
    private static final Set<String> SPAM_KEYWORDS = Set.of(
        "matricula", "matriculas", "matriculate",
        "descuento", "descuentos",
        "pago", "pagos",
        "precio", "precios", "costo", "costos", "valor",
        "oferta", "ofertas",
        "promo", "promocion", "promociones",
        "inscripciones", "inscripcion",
        "cupo", "cupos", "cupos limitados",
        "curso", "cursos",
        "link en bio", "enlace en bio",
        "formulario de registro",
        "regístrate ya", "registrate ya"
    );

    // ── Palabras clave de EVENTOS ────────────────────────────────────
    private static final Set<String> EVENT_KEYWORDS = Set.of(
        // Señales temporales
        "hora", "horario", "fecha", "agenda", "programa",
        // Señales de convocatoria
        "invitacion", "invitados", "asiste", "asistan", "bienvenidos",
        "te invitamos", "los invitamos", "estan invitados",
        // Tipos de evento
        "evento", "actividad", "conferencia", "charla", "taller",
        "foro", "seminario", "ponencia", "congreso", "simposio",
        "ceremonia", "graduacion", "graduaciones", "bienvenida",
        "clausura", "apertura", "inauguracion", "exposicion",
        "muestra", "concurso", "competencia", "campeonato",
        "debate", "panel", "coloquio", "webinar", "capacitacion",
        // Señales de lugar (zonas del campus UCSG)
        "auditorio", "biblioteca", "cancha", "cafeteria", "salon",
        "aula", "laboratorio", "bloque", "facultad", "rectorado",
        "parqueadero", "plaza", "patio",
        // Señales de participación
        "participa", "participen", "asistencia", "confirmacion",
        "presencia", "no faltes", "no te pierdas", "se parte"
    );

    // ── API pública ──────────────────────────────────────────────────

    /**
     * Clasifica el caption y devuelve la acción a tomar.
     *
     * @param caption texto del post de Instagram (puede ser null)
     * @return {@link ClassificationResult} — nunca null
     */
    public ClassificationResult classify(String caption) {
        if (caption == null || caption.isBlank()) {
            return ClassificationResult.SKIP;
        }

        String normalized = normalize(caption);

        long spamHits = SPAM_KEYWORDS.stream()
            .filter(normalized::contains)
            .count();

        if (spamHits >= SPAM_THRESHOLD) {
            log.debug("Caption clasificado como SPAM ({} hits): …{}…",
                spamHits, caption.substring(0, Math.min(caption.length(), 60)));
            return ClassificationResult.AUTO_REJECTED;
        }

        long eventHits = EVENT_KEYWORDS.stream()
            .filter(normalized::contains)
            .count();

        if (eventHits >= 1) {
            log.debug("Caption clasificado como EVENTO ({} hits)", eventHits);
            return ClassificationResult.EVENT;
        }

        log.debug("Caption sin señales claras — SKIP");
        return ClassificationResult.SKIP;
    }

    // ── Privados ─────────────────────────────────────────────────────

    /**
     * Normaliza el texto: minúsculas + elimina acentos + colapsa espacios.
     * Permite que las keywords sin tildes coincidan con texto con tildes y viceversa.
     */
    private String normalize(String text) {
        String noAccents = Normalizer
            .normalize(text.toLowerCase(), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        return noAccents.replaceAll("\\s+", " ").trim();
    }
}
