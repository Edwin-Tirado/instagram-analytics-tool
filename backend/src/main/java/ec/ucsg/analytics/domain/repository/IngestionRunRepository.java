package ec.ucsg.analytics.domain.repository;

import ec.ucsg.analytics.domain.model.IngestionRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IngestionRunRepository extends JpaRepository<IngestionRun, UUID> {

    Page<IngestionRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
