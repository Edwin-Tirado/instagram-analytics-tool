package ec.ucsg.analytics.infrastructure.instagram;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad estática para extraer metadatos estructurados
 * de los captions de Instagram en español.
 *
 * No usa IA ni librerías externas — solo reglas y regex.
 * La cobertura es pragmática: cubre los formatos más comunes
 * usados por @ucsgnotificaciones, no todos los casos posibles.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CaptionParser {

    // ── Meses en español ────────────────────────────────────────────
    private static final Map<String, Integer> MONTH_MAP = Map.ofEntries(
        Map.entry("enero",      1),  Map.entry("febrero",   2),
        Map.entry("marzo",      3),  Map.entry("abril",     4),
        Map.entry("mayo",       5),  Map.entry("junio",     6),
        Map.entry("julio",      7),  Map.entry("agosto",    8),
        Map.entry("septiembre", 9),  Map.entry("setiembre", 9),
        Map.entry("octubre",   10),  Map.entry("noviembre", 11),
        Map.entry("diciembre", 12)
    );

    // "25 de junio de 2026" o "25 de junio"
    private static final Pattern DATE_LONG =
        Pattern.compile("(\\d{1,2})\\s+de\\s+(\\w+)(?:\\s+de\\s+(\\d{4}))?", Pattern.CASE_INSENSITIVE);

    // "25/06/2026" o "25-06-2026"
    private static final Pattern DATE_NUMERIC =
        Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{2,4})");

    // "10:00" o "10h00" o "10h" — hora del evento
    private static final Pattern TIME_PATTERN =
        Pattern.compile("(\\d{1,2})(?:h|:)(\\d{0,2})(?:\\s*(?:am|pm))?", Pattern.CASE_INSENSITIVE);

    // ── Extracción de título ────────────────────────────────────────

    /**
     * Extrae el título del caption: primera línea no vacía,
     * excluyendo hashtags (#) y menciones (@) puros.
     * Trunca a 255 caracteres.
     */
    public static String extractTitle(String caption) {
        if (caption == null || caption.isBlank()) {
            return "Evento sin título";
        }

        return Arrays.stream(caption.split("\n"))
            .map(String::trim)
            .filter(line -> !line.isBlank()
                         && !line.startsWith("#")
                         && !line.startsWith("@")
                         && line.length() >= 5)
            .findFirst()
            .map(line -> line.length() > 255 ? line.substring(0, 252) + "…" : line)
            .orElse("Evento sin título");
    }

    // ── Extracción de fecha y hora ──────────────────────────────────

    /**
     * Intenta extraer una {@link LocalDateTime} del caption.
     * Estrategia:
     *   1. Buscar fecha en formato largo ("25 de junio de 2026").
     *   2. Buscar fecha numérica ("25/06/2026").
     *   3. Combinar con hora si se encuentra.
     *   4. Devolver {@link Optional#empty()} si no hay fecha parseable.
     */
    public static Optional<LocalDateTime> extractDateTime(String caption) {
        if (caption == null || caption.isBlank()) {
            return Optional.empty();
        }

        String normalized = caption.toLowerCase();

        Optional<LocalDate> date = parseLongDate(normalized)
            .or(() -> parseNumericDate(normalized));

        if (date.isEmpty()) {
            return Optional.empty();
        }

        LocalTime time = parseTime(normalized).orElse(LocalTime.of(8, 0));
        return Optional.of(LocalDateTime.of(date.get(), time));
    }

    // ── Privados ────────────────────────────────────────────────────

    private static Optional<LocalDate> parseLongDate(String text) {
        Matcher m = DATE_LONG.matcher(text);
        if (!m.find()) return Optional.empty();

        try {
            int day   = Integer.parseInt(m.group(1));
            int month = Optional.ofNullable(MONTH_MAP.get(m.group(2).toLowerCase()))
                                .orElse(-1);
            if (month == -1 || day < 1 || day > 31) return Optional.empty();

            int year = m.group(3) != null
                ? Integer.parseInt(m.group(3))
                : resolveYear(month, day);

            return Optional.of(LocalDate.of(year, month, day));
        } catch (Exception e) {
            log.debug("No se pudo parsear fecha larga: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> parseNumericDate(String text) {
        Matcher m = DATE_NUMERIC.matcher(text);
        if (!m.find()) return Optional.empty();

        try {
            int day   = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int year  = Integer.parseInt(m.group(3));
            if (year < 100) year += 2000;

            return Optional.of(LocalDate.of(year, month, day));
        } catch (Exception e) {
            log.debug("No se pudo parsear fecha numérica: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<LocalTime> parseTime(String text) {
        Matcher m = TIME_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();

        try {
            int hour   = Integer.parseInt(m.group(1));
            String minStr = m.group(2);
            int minute = (minStr != null && !minStr.isBlank()) ? Integer.parseInt(minStr) : 0;

            if (hour > 23 || minute > 59) return Optional.empty();
            return Optional.of(LocalTime.of(hour, minute));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Si el año no está en el caption, elige el año más próximo
     * en que esa fecha (mes/día) ya no haya pasado.
     */
    private static int resolveYear(int month, int day) {
        int currentYear = Year.now().getValue();
        LocalDate candidate = LocalDate.of(currentYear, month, day);
        return candidate.isBefore(LocalDate.now())
            ? currentYear + 1
            : currentYear;
    }
}
