package ec.ucsg.analytics.infrastructure.instagram;

import ec.ucsg.analytics.domain.model.EventImage;
import ec.ucsg.analytics.domain.repository.EventImageRepository;
import ec.ucsg.analytics.infrastructure.instagram.dto.InstagramMediaItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Refresca las mediaUrl de Instagram cuando ya expiraron (~7 días).
 *
 * Extraído de {@code EventIngestionService.refreshAllExpiredUrlsAsync} para que
 * tanto el refresco masivo (disparado desde el panel admin) como el refresco
 * puntual bajo demanda ({@link #refreshSingleImageUrl}, usado al armar un correo
 * en {@code EmailNotificationService}) compartan la misma lógica de emparejamiento
 * carrusel/imagen simple — sin que infraestructura (mail) tenga que depender de
 * la capa de aplicación (ingesta), que rompería la dirección de dependencias del
 * proyecto (application → infrastructure, nunca al revés).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramImageUrlRefresher {

    private final InstagramGraphApiClient apiClient;
    private final EventImageRepository    eventImageRepository;

    /**
     * Refresca todas las imágenes de un post (carrusel o simple), emparejando
     * por posición (displayOrder) cuando aplica. Fail-safe: cualquier error de
     * red o de la API de Instagram se registra y se devuelve 0, nunca se propaga.
     *
     * @return número de imágenes efectivamente actualizadas.
     */
    @Transactional
    public int refreshUrlsForPost(String postId, List<EventImage> images) {
        try {
            List<InstagramMediaItem> children = apiClient.fetchChildren(postId);
            List<String> freshUrls = children.stream()
                .filter(c -> c.getMediaUrl() != null && !"VIDEO".equals(c.getMediaType()))
                .map(InstagramMediaItem::getMediaUrl)
                .collect(Collectors.toList());

            int updated = 0;

            if (!freshUrls.isEmpty()) {
                // Es un carrusel — emparejar por posición (displayOrder).
                List<EventImage> sortedImages = images.stream()
                    .sorted(Comparator.comparingInt(EventImage::getDisplayOrder))
                    .collect(Collectors.toList());

                for (int i = 0; i < sortedImages.size(); i++) {
                    String freshUrl = freshUrls.get(Math.min(i, freshUrls.size() - 1));
                    EventImage img = sortedImages.get(i);
                    if (!freshUrl.equals(img.getMediaUrl())) {
                        img.setMediaUrl(freshUrl);
                        eventImageRepository.save(img);
                        updated++;
                    }
                }
            } else {
                // Post de imagen simple — pedir su URL fresca directamente.
                Optional<InstagramMediaItem> single = apiClient.fetchSingleMedia(postId);
                if (single.isPresent()) {
                    InstagramMediaItem item = single.get();
                    String freshUrl = item.getMediaUrl();
                    if (freshUrl != null && !"VIDEO".equals(item.getMediaType())) {
                        for (EventImage img : images) {
                            if (!freshUrl.equals(img.getMediaUrl())) {
                                img.setMediaUrl(freshUrl);
                                eventImageRepository.save(img);
                                updated++;
                            }
                        }
                    }
                }
            }
            return updated;
        } catch (Exception e) {
            log.warn("No se pudo refrescar el post {}: {}", postId, e.getMessage());
            return 0;
        }
    }

    /**
     * Refresca bajo demanda la mediaUrl de una imagen puntual (y sus hermanas del
     * mismo post de Instagram, si es un carrusel). Pensado para el momento de
     * adjuntar la imagen a un correo, cuando la URL almacenada ya expiró y no
     * hay por qué esperar a que un admin dispare el refresco masivo manualmente.
     *
     * @return la URL fresca de la imagen solicitada, vacío si no se pudo refrescar.
     */
    @Transactional
    public Optional<String> refreshSingleImageUrl(Long eventImageId) {
        EventImage target = eventImageRepository.findById(eventImageId).orElse(null);
        if (target == null || target.getSourceInstagramPostId() == null) {
            return Optional.empty();
        }

        List<EventImage> siblings =
            eventImageRepository.findBySourceInstagramPostId(target.getSourceInstagramPostId());
        refreshUrlsForPost(target.getSourceInstagramPostId(), siblings);

        return eventImageRepository.findById(eventImageId).map(EventImage::getMediaUrl);
    }
}
