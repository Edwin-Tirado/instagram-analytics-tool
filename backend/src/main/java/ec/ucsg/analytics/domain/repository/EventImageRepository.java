package ec.ucsg.analytics.domain.repository;

import ec.ucsg.analytics.domain.model.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventImageRepository extends JpaRepository<EventImage, Long> {

    /** Previene la re-ingesta de un post ya procesado. */
    boolean existsBySourceInstagramPostId(String sourceInstagramPostId);

    /**
     * Todas las imágenes asociadas a un post de Instagram.
     * Usado para refrescar las mediaUrl expiradas durante la ingesta.
     */
    List<EventImage> findBySourceInstagramPostId(String sourceInstagramPostId);
}

