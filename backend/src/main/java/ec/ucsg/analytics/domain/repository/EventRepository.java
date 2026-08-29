package ec.ucsg.analytics.domain.repository;

import ec.ucsg.analytics.domain.enums.EventStatus;
import ec.ucsg.analytics.domain.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Optional<Event> findByInstagramPostId(String instagramPostId);

    /**
     * Búsqueda antiduplicados: mismo título + misma zona + fecha dentro
     * de una ventana de ±1 hora para tolerar leves variaciones en la
     * fecha parseada del caption.
     */
    @Query("""
        SELECT e FROM Event e
        WHERE LOWER(e.title) = LOWER(:title)
          AND e.zone.id       = :zoneId
          AND e.eventDate BETWEEN :dateFrom AND :dateTo
        """)
    Optional<Event> findDuplicate(
        @Param("title")    String title,
        @Param("zoneId")   Long zoneId,
        @Param("dateFrom") LocalDateTime dateFrom,
        @Param("dateTo")   LocalDateTime dateTo
    );

    /**
     * Eventos APROBADOS y aún no finalizados para la cartelera pública.
     * Oculta eventos con fecha ya pasada — un evento vencido no le sirve a
     * nadie en la vista pública y solo genera confusión ("¿por qué me
     * anuncian algo que ya pasó?"). Sigue mostrando los que no tienen fecha
     * (CaptionParser no la encontró): el admin/supervisor los ve y corrige
     * en su panel, que no filtra por fecha.
     */
    @Query("""
        SELECT e FROM Event e
        WHERE e.status = 'APPROVED'
          AND (e.eventDate IS NULL OR e.eventDate >= CURRENT_TIMESTAMP)
        ORDER BY e.eventDate ASC NULLS LAST
        """)
    Page<Event> findUpcomingApproved(Pageable pageable);
}
