package ec.ucsg.analytics.application.service;

import ec.ucsg.analytics.domain.enums.EventStatus;
import ec.ucsg.analytics.domain.model.Event;
import ec.ucsg.analytics.domain.model.EventImage;
import ec.ucsg.analytics.domain.model.Zone;
import ec.ucsg.analytics.domain.repository.EventImageRepository;
import ec.ucsg.analytics.domain.repository.EventRepository;
import ec.ucsg.analytics.infrastructure.instagram.CaptionParser;
import ec.ucsg.analytics.infrastructure.instagram.InstagramGraphApiClient;
import ec.ucsg.analytics.infrastructure.instagram.dto.InstagramMediaItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Orquestador del pipeline de ingesta de posts de Instagram.
 *
 * Pipeline por cada post:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 1. Guardia antiduplicados (¿imagen ya procesada?)           │
 * │ 2. Clasificación heurística (EVENT / AUTO_REJECTED / SKIP)  │
 * │ 3. Extracción de título y fecha del caption                  │
 * │ 4. Matching de zona por keywords                             │
 * │ 5. Detección de evento duplicado (título + zona + fecha ±1h) │
 * │ 6a. Duplicado → añadir imagen al carrusel existente          │
 * │ 6b. Nuevo     → crear Event(PENDING) + EventImage            │
 * │ 6c. Spam      → crear Event(REJECTED) con motivo automático  │
 * └─────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventIngestionService {

    private static final String AUTO_REJECT_REASON =
        "AUTO: contenido publicitario o spam detectado por el clasificador";

    private final InstagramGraphApiClient  apiClient;
    private final EventClassificationService classificationService;
    private final ZoneMatchingService      zoneMatchingService;
    private final EventRepository          eventRepository;
    private final EventImageRepository     eventImageRepository;

    // ── Punto de entrada (llamado desde el CRON job) ────────────────

    /**
     * Recupera todos los posts recientes de Instagram y los procesa.
     * Transaccional por post individual para que un fallo aislado no
     * revierta los eventos ya guardados en la misma ejecución.
     */
    public void runIngestion() {
        List<InstagramMediaItem> posts = apiClient.fetchRecentPosts();
        log.info("Iniciando procesamiento de {} posts", posts.size());

        int created = 0, merged = 0, rejected = 0, skipped = 0;

        for (InstagramMediaItem post : posts) {
            try {
                IngestionOutcome outcome = processSinglePost(post);
                switch (outcome) {
                    case CREATED  -> created++;
                    case MERGED   -> merged++;
                    case REJECTED -> rejected++;
                    case SKIPPED  -> skipped++;
                }
            } catch (Exception e) {
                log.error("Error procesando post {}: {}", post.getId(), e.getMessage(), e);
            }
        }

        log.info("Ingesta finalizada — creados: {}, fusionados: {}, rechazados: {}, omitidos: {}",
            created, merged, rejected, skipped);
    }

    // ── Pipeline por post ────────────────────────────────────────────

    @Transactional
    public IngestionOutcome processSinglePost(InstagramMediaItem post) {

        // 1. Guardia — ¿ya procesamos alguna imagen de este post?
        if (eventImageRepository.existsBySourceInstagramPostId(post.getId())) {
            log.debug("Post ya procesado, omitiendo: {}", post.getId());
            return IngestionOutcome.SKIPPED;
        }

        String caption = post.getCaption() != null ? post.getCaption() : "";

        // 2. Clasificación heurística
        ClassificationResult classification = classificationService.classify(caption);

        if (classification == ClassificationResult.SKIP) {
            log.debug("Post sin señales de evento, omitiendo: {}", post.getId());
            return IngestionOutcome.SKIPPED;
        }

        // 3. Extraer metadatos del caption
        String        title     = CaptionParser.extractTitle(caption);
        LocalDateTime eventDate = CaptionParser.extractDateTime(caption).orElse(null);

        // 4. Matching de zona
        Optional<Zone> zone = zoneMatchingService.findBestMatch(caption);

        // 5. Manejar spam antes de buscar duplicados
        if (classification == ClassificationResult.AUTO_REJECTED) {
            persistRejectedEvent(post, title, caption, zone.orElse(null), eventDate);
            return IngestionOutcome.REJECTED;
        }

        // 6. Buscar evento duplicado (solo si tenemos zona y fecha)
        if (zone.isPresent() && eventDate != null) {
            Optional<Event> existing = eventRepository.findDuplicate(
                title,
                zone.get().getId(),
                eventDate.minusHours(1),
                eventDate.plusHours(1)
            );

            if (existing.isPresent()) {
                appendImagesToEvent(existing.get(), post);
                eventRepository.save(existing.get());
                log.info("Imagen fusionada en evento existente '{}' ({})", title, existing.get().getId());
                return IngestionOutcome.MERGED;
            }
        }

        // 7. Crear nuevo evento APROBADO (publicación automática)
        Event newEvent = Event.builder()
            .title(title)
            .caption(caption)
            .locationText(zone.map(Zone::getName).orElse(null))
            .zone(zone.orElse(null))
            .eventDate(eventDate)
            .status(EventStatus.APPROVED)
            .instagramPostId(post.getId())
            .build();

        appendImagesToEvent(newEvent, post);
        eventRepository.save(newEvent);
        log.info("Nuevo evento PENDIENTE creado: '{}' (postId={})", title, post.getId());
        return IngestionOutcome.CREATED;
    }

    // ── Privados ─────────────────────────────────────────────────────

    private void persistRejectedEvent(
            InstagramMediaItem post, String title,
            String caption, Zone zone, LocalDateTime eventDate) {

        Event spam = Event.builder()
            .title(title)
            .caption(caption)
            .locationText(zone != null ? zone.getName() : null)
            .zone(zone)
            .eventDate(eventDate)
            .status(EventStatus.REJECTED)
            .instagramPostId(post.getId())
            .rejectionReason(AUTO_REJECT_REASON)
            .build();

        appendImagesToEvent(spam, post);
        eventRepository.save(spam);
        log.info("Post rechazado automáticamente (spam): {}", post.getId());
    }

    /**
     * Añade las imágenes del post al evento.
     * Para CAROUSEL_ALBUM obtiene los hijos de la API.
     * Para IMAGE/VIDEO usa directamente el media_url del post padre.
     */
    private void appendImagesToEvent(Event event, InstagramMediaItem post) {
        int order = event.getImages().size();

        if ("CAROUSEL_ALBUM".equals(post.getMediaType())) {
            List<InstagramMediaItem> children = apiClient.fetchChildren(post.getId());

            for (InstagramMediaItem child : children) {
                if (child.getMediaUrl() == null) continue;
                if ("VIDEO".equals(child.getMediaType())) continue; // solo imágenes en el carrusel

                event.addImage(EventImage.builder()
                    .mediaUrl(child.getMediaUrl())
                    .sourceInstagramPostId(post.getId())
                    .displayOrder(order++)
                    .build());
            }

        } else if (post.getMediaUrl() != null && !"VIDEO".equals(post.getMediaType())) {
            event.addImage(EventImage.builder()
                .mediaUrl(post.getMediaUrl())
                .sourceInstagramPostId(post.getId())
                .displayOrder(order)
                .build());
        }
    }

    // ── Resultado de la operación ────────────────────────────────────

    public enum IngestionOutcome {
        CREATED,    // Nuevo evento PENDING guardado
        MERGED,     // Imagen añadida a evento existente
        REJECTED,   // Spam — guardado como REJECTED
        SKIPPED     // Sin señales o ya procesado
    }
}
