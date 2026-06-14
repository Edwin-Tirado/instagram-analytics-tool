package ec.ucsg.analytics.domain.repository;

import ec.ucsg.analytics.domain.enums.RoleName;
import ec.ucsg.analytics.domain.model.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByName(RoleName name);
}
