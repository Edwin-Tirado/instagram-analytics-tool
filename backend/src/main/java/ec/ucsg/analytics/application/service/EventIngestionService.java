package ec.ucsg.analytics.application.service;

import ec.ucsg.analytics.application.dto.response.IngestionRunResponse;
import ec.ucsg.analytics.application.dto.response.PageResponse;
import ec.ucsg.analytics.domain.enums.EventStatus;
import ec.ucsg.analytics.domain.model.Event;
import ec.ucsg.analytics.domain.model.EventImage;
import ec.ucsg.analytics.domain.model.IngestionRun;
import ec.ucsg.analytics.domain.model.Zone;
import ec.ucsg.analytics.domain.repository.EventImageRepository;
import ec.ucsg.analytics.domain.repository.EventRepository;
import ec.ucsg.analytics.domain.repository.IngestionRunRepository;
import ec.ucsg.analytics.infrastructure.instagram.CaptionParser;
import ec.ucsg.analytics.infrastructure.instagram.InstagramGraphApiClient;
import ec.ucsg.analytics.infrastructure.instagram.dto.InstagramMediaItem;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * │ 6b. Nuevo     → crear Event(APPROVED) + EventImage — se       │
 * │                 autopublica, el supervisor solo edita/elimina │
 * │ 6c. Spam      → crear Event(REJECTED) con motivo automático  │
 * └─────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventIngestionService {

    private static final String AUTO_REJECT_REASON =
        "AUTO: contenido publicitario o spam detectado por el clasificador";

    private final InstagramGraphApiClient    apiClient;
    private final EventClassificationService classificationService;
    private final ZoneMatchingService        zoneMatchingService;
    private final EventRepository            eventRepository;
    private final EventImageRepository       eventImageRepository;
    private final IngestionRunRepository     ingestionRunRepository;

    // ── Punto de entrada (llamado desde el CRON job y el AdminController) ──

    /**
     * Ejecuta el pipeline completo de ingesta de forma síncrona y persiste el
     * resultado en {@link IngestionRun}. Usado por el CRON ({@link
     * ec.ucsg.analytics.infrastructure.scheduling.InstagramIngestionJob}), que
     * ya corre en su propio hilo de scheduling — bloquear ahí no afecta al dashboard.
     *
     * @param triggerType SCHEDULED (CRON) o MANUAL (uso en tests/consola)
     * @return la entidad {@link IngestionRun} con el resultado final
     */
    public IngestionRun runIngestion(IngestionRun.TriggerType triggerType) {
        IngestionRun run = createRun(triggerType);
        executeIngestionPipeline(run);
        return ingestionRunRepository.save(run);
    }

    /**
     * Crea el registro RUNNING sin ejecutar el pipeline — permite responder
     * de inmediato al disparo manual desde el dashboard mientras el trabajo
     * pesado (llamadas a la API de Instagram) corre en segundo plano.
     */
    public IngestionRun createRun(IngestionRun.TriggerType triggerType) {
        return ingestionRunRepository.save(
            IngestionRun.builder()
                .startedAt(LocalDateTime.now())
                .triggerType(triggerType)
                .build()
        );
    }

    /**
     * Ejecuta el pipeline en segundo plano para un run ya creado con {@link #createRun}.
     *
     * IMPORTANTE: debe invocarse desde OTRO bean (p. ej. el controller), nunca
     * con {@code this.executeIngestionAsync(...)} dentro de esta misma clase —
     * el proxy de Spring que activa {@code @Async} no intercepta llamadas internas
     * ("self-invocation"), y el método se ejecutaría de forma síncrona igual.
     */
    @Async
    public void executeIngestionAsync(UUID runId) {
        IngestionRun run = ingestionRunRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("Ejecución no encontrada: " + runId));
        executeIngestionPipeline(run);
        ingestionRunRepository.save(run);
    }

    /** Lógica compartida del pipeline — deja el resultado escrito en {@code run} (sin persistir). */
    private void executeIngestionPipeline(IngestionRun run) {
        try {
            List<InstagramMediaItem> posts = apiClient.fetchRecentPosts();
            log.info("Procesando {} posts (trigger={})", posts.size(), run.getTriggerType());

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

            run.complete(created, merged, rejected, skipped);
            log.info("Ingesta finalizada — creados: {}, fusionados: {}, rechazados: {}, omitidos: {}",
                created, merged, rejected, skipped);

        } catch (Exception e) {
            // La API de Instagram falló (o no hay posts) — se registra en la auditoría
            // (IngestionRun.FAILED + errorMessage) sin detener el resto del dashboard.
            run.fail(e.getMessage());
            log.error("El pipeline de ingesta terminó con error: {}", e.getMessage(), e);
        }
    }

    // ── Consultas de historial (usadas por AdminIngestionController) ─

    public PageResponse<IngestionRunResponse> getRunHistory(Pageable pageable) {
        Page<IngestionRunResponse> page = ingestionRunRepository
            .findAllByOrderByStartedAtDesc(pageable)
            .map(IngestionRunResponse::from);
        return PageResponse.of(page);
    }

    public IngestionRunResponse getRunById(UUID id) {
        return ingestionRunRepository.findById(id)
            .map(IngestionRunResponse::from)
            .orElseThrow(() -> new EntityNotFoundException("Ejecución no encontrada: " + id));
    }

    // ── Pipeline por post ────────────────────────────────────────────

    @Transactional
    public IngestionOutcome processSinglePost(InstagramMediaItem post) {

        // 1. Guardia — ¿ya procesamos alguna imagen de este post?
        if (eventImageRepository.existsBySourceInstagramPostId(post.getId())) {
            // Refrescar la mediaUrl expirada con la URL fresca que viene en el post actual.
            // Esto ocurre en el mismo request a la API (sin llamada adicional): aprovechamos
            // que el cron ya trajo el post con su media_url actualizada.
            refreshImageUrls(post);
            log.debug("Post ya procesado, URLs refrescadas: {}", post.getId());
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

        // 7. Crear nuevo evento — se autopublica (APPROVED) de inmediato.
        // El supervisor ya no aprueba/rechaza lo que viene de Instagram; solo
        // edita o elimina después si hace falta corregir algo (título, zona,
        // fecha) o quitar un post que no debió publicarse.
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
        log.info("Nuevo evento autopublicado: '{}' (postId={})", title, post.getId());
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

    /**
     * Refresca las mediaUrl de todas las imágenes vinculadas a un post de Instagram.
     *
     * Las URLs de Instagram expiran después de ~7 días. El pipeline de ingesta
     * llama a este método cuando detecta que el post ya fue procesado (SKIPPED),
     * aprovechando la URL fresca que la API de Instagram acaba de devolver
     * sin necesidad de una llamada HTTP adicional.
     *
     * Para CAROUSEL_ALBUM: los hijos se ordenan por su posición natural de la API
     * y se emparejan en orden con las imágenes almacenadas (ordenadas por displayOrder).
     * Para IMAGE: la URL fresca viene directamente del media_url del post padre.
     */
    private void refreshImageUrls(InstagramMediaItem post) {
        try {
            if ("CAROUSEL_ALBUM".equals(post.getMediaType())) {
                // Obtener hijos frescos — la API los devuelve en el mismo orden que cuando
                // se ingirieron originalmente.
                List<InstagramMediaItem> children = apiClient.fetchChildren(post.getId());
                List<String> freshUrls = children.stream()
                    .filter(c -> c.getMediaUrl() != null && !"VIDEO".equals(c.getMediaType()))
                    .map(InstagramMediaItem::getMediaUrl)
                    .collect(Collectors.toList());

                if (freshUrls.isEmpty()) return;

                // Ordenar las imágenes almacenadas para mantener la correspondencia posicional.
                List<EventImage> images = eventImageRepository
                    .findBySourceInstagramPostId(post.getId())
                    .stream()
                    .sorted(Comparator.comparingInt(EventImage::getDisplayOrder))
                    .collect(Collectors.toList());

                for (int i = 0; i < images.size(); i++) {
                    // Si hay menos hijos frescos que imágenes almacenadas, usar el último
                    // URL fresco disponible como respaldo para los sobrantes.
                    String freshUrl = freshUrls.get(Math.min(i, freshUrls.size() - 1));
                    EventImage img = images.get(i);
                    if (!freshUrl.equals(img.getMediaUrl())) {
                        img.setMediaUrl(freshUrl);
                        eventImageRepository.save(img);
                    }
                }

            } else if (post.getMediaUrl() != null) {
                List<EventImage> images =
                    eventImageRepository.findBySourceInstagramPostId(post.getId());
                for (EventImage img : images) {
                    if (!post.getMediaUrl().equals(img.getMediaUrl())) {
                        img.setMediaUrl(post.getMediaUrl());
                        eventImageRepository.save(img);
                    }
                }
            }
        } catch (Exception e) {
            // Fail-safe: el refresco de URLs es una mejora de calidad, nunca debe
            // interrumpir el pipeline principal ni crear un IngestionRun FAILED.
            log.warn("No se pudieron refrescar las URLs del post {}: {}", post.getId(), e.getMessage());
        }
    }

    /**
     * Escanea TODAS las imágenes almacenadas, las agrupa por sourceInstagramPostId
     * y re-obtiene URLs frescas desde la Instagram API para cada post único.
     *
     * Diseñado para ser invocado desde el endpoint de administración
     * {@code POST /api/admin/ingestion/refresh-urls} cuando las URLs ya expiraron
     * y el CRON normal no las alcanza (posts fuera de la ventana daysLookback).
     *
     * @return número de imágenes efectivamente actualizadas
     */
    @Async
    @Transactional
    public void refreshAllExpiredUrlsAsync() {
        log.info("[RefreshURLs] Iniciando refresco masivo de URLs de imágenes...");

        // Agrupar todas las imágenes por su post de Instagram fuente
        List<EventImage> allImages = eventImageRepository.findAll();
        Map<String, List<EventImage>> byPostId = allImages.stream()
            .filter(img -> img.getSourceInstagramPostId() != null)
            .collect(Collectors.groupingBy(EventImage::getSourceInstagramPostId));

        int totalUpdated = 0;
        int totalPosts   = byPostId.size();
        int processed    = 0;

        for (Map.Entry<String, List<EventImage>> entry : byPostId.entrySet()) {
            String postId = entry.getKey();
            List<EventImage> images = entry.getValue();
            processed++;

            try {
                // Intentar obtener los hijos (carrusel) primero.
                // Si el post es una imagen simple, fetchChildren devolverá lista vacía.
                List<InstagramMediaItem> children = apiClient.fetchChildren(postId);
                List<String> freshUrls = children.stream()
                    .filter(c -> c.getMediaUrl() != null && !"VIDEO".equals(c.getMediaType()))
                    .map(InstagramMediaItem::getMediaUrl)
                    .collect(Collectors.toList());

                if (!freshUrls.isEmpty()) {
                    // Es un carrusel — emparejar por posición (displayOrder)
                    List<EventImage> sortedImages = images.stream()
                        .sorted(Comparator.comparingInt(EventImage::getDisplayOrder))
                        .collect(Collectors.toList());

                    for (int i = 0; i < sortedImages.size(); i++) {
                        String freshUrl = freshUrls.get(Math.min(i, freshUrls.size() - 1));
                        EventImage img = sortedImages.get(i);
                        if (!freshUrl.equals(img.getMediaUrl())) {
                            img.setMediaUrl(freshUrl);
                            eventImageRepository.save(img);
                            totalUpdated++;
                        }
                    }
                }
                // Nota: para posts de imagen simple, la única forma de obtener la URL
                // fresca es a través del endpoint /{mediaId}?fields=media_url, que no
                // está en el cliente actual. Esos posts se refrescan automáticamente
                // durante la próxima ejecución del CRON (si están dentro de daysLookback).

            } catch (Exception e) {
                log.warn("[RefreshURLs] No se pudo refrescar post {}: {}", postId, e.getMessage());
            }

            if (processed % 10 == 0) {
                log.info("[RefreshURLs] Progreso: {}/{} posts procesados, {} URLs actualizadas",
                    processed, totalPosts, totalUpdated);
            }
        }

        log.info("[RefreshURLs] Completado: {}/{} posts procesados, {} URLs actualizadas en total",
            processed, totalPosts, totalUpdated);
    }

    // ── Resultado de la operación ───────────────────────────────────────────────

    public enum IngestionOutcome {
        CREATED,    // Nuevo evento PENDING guardado
        MERGED,     // Imagen añadida a evento existente
        REJECTED,   // Spam — guardado como REJECTED
        SKIPPED     // Sin señales o ya procesado
    }
}
