package ec.ucsg.analytics.domain.repository;

import ec.ucsg.analytics.domain.model.InstagramToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstagramTokenRepository extends JpaRepository<InstagramToken, Integer> {
}
