package ec.ucsg.analytics.application.service;

import ec.ucsg.analytics.domain.model.Zone;
import ec.ucsg.analytics.domain.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Algoritmo de asignación automática de zona a partir del caption.
 *
 * Estrategia de scoring:
 *   Para cada {@link Zone} activa en la BD, se tokeniza su campo
 *   {@code matchKeywords} (coma-separado) y se cuenta cuántos tokens
 *   aparecen en el caption normalizado.
 *   La zona con mayor score >= 1 gana.
 *   Si hay empate, se prefiere la zona cuyo nombre completo aparece.
 *
 * Zonas del campus UCSG (fuente: scribblemaps.com/maps/view/UCSG-MAPS/D8IRPbqRGi):
 *   Auditorio Principal, Biblioteca, Cancha Deportiva, Cafetería,
 *   Bloque Administrativo, Facultad de Ingeniería, y las demás
 *   definidas en V1__init_schema.sql.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneMatchingService {

    private final ZoneRepository zoneRepository;

    @Transactional(readOnly = true)
    public Optional<Zone> findBestMatch(String caption) {
        if (caption == null || caption.isBlank()) {
            return Optional.empty();
        }

        String normalizedCaption = normalize(caption);
        List<Zone> allZones = zoneRepository.findAll();

        return allZones.stream()
            .map(zone -> new ScoredZone(zone, score(zone, normalizedCaption)))
            .filter(sz -> sz.score() > 0)
            .max(Comparator.comparingInt(ScoredZone::score))
            .map(ScoredZone::zone);
    }

    // ── Privados ─────────────────────────────────────────────────────

    /**
     * Calcula cuántos tokens de las keywords de la zona aparecen en el caption.
     * Tokens más largos pesan más (se multiplica por la longitud del token)
     * para que "auditorio principal" pese más que solo "auditorio".
     */
    private int score(Zone zone, String normalizedCaption) {
        if (zone.getMatchKeywords() == null || zone.getMatchKeywords().isBlank()) {
            return 0;
        }

        return Arrays.stream(zone.getMatchKeywords().split(","))
            .map(String::trim)
            .filter(kw -> !kw.isBlank())
            .mapToInt(kw -> {
                String normalizedKw = normalize(kw);
                return normalizedCaption.contains(normalizedKw) ? normalizedKw.length() : 0;
            })
            .sum();
    }

    private String normalize(String text) {
        return Normalizer
            .normalize(text.toLowerCase(), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private record ScoredZone(Zone zone, int score) {}
}
