package ec.ucsg.analytics.domain.repository;

import ec.ucsg.analytics.domain.model.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    Optional<Zone> findByNameIgnoreCase(String name);

    /**
     * Busca todas las zonas cuyo campo matchKeywords contenga alguno de los
     * tokens del caption (búsqueda ILIKE). El servicio de aplicación elige
     * la zona con mayor número de coincidencias.
     */
    @Query("""
        SELECT z FROM Zone z
        WHERE LOWER(z.matchKeywords) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    List<Zone> findByKeyword(@Param("keyword") String keyword);
}
