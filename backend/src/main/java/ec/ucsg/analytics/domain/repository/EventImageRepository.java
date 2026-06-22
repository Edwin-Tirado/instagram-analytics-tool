package ec.ucsg.analytics.domain.repository;

import ec.ucsg.analytics.domain.model.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventImageRepository extends JpaRepository<EventImage, Long> {

    /** Previene la re-ingesta de un post ya procesado. */
    boolean existsBySourceInstagramPostId(String sourceInstagramPostId);
}
