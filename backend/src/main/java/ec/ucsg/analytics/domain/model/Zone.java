package ec.ucsg.analytics.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Zona espacial del campus (estática).
 * La geometría usa PostGIS via Hibernate Spatial (SRID 4326 — WGS84).
 * Los nombres deben coincidir con las etiquetas del mapa de referencia
 * (scribblemaps.com/maps/view//D8IRPbqRGi) para habilitar el
 * auto-asignado de eventos por keyword matching en el caption.
 */
@Entity
@Table(
    name = "zones",
    indexes = {
        @Index(name = "idx_zones_name", columnList = "name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name"})
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    /**
     * Geometría Point (lon, lat) almacenada en PostGIS.
     * Requiere la extensión PostGIS activa en la BD:
     *   CREATE EXTENSION IF NOT EXISTS postgis;
     */
    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    /**
     * Keywords separadas por coma para el auto-matching con captions
     * de Instagram. Ej: "auditorio,auditorio principal,edificio a"
     */
    @Column(name = "match_keywords", length = 500)
    private String matchKeywords;

    @OneToMany(mappedBy = "zone", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Event> events = new ArrayList<>();
}
