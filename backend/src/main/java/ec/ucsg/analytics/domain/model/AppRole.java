package ec.ucsg.analytics.domain.model;

import ec.ucsg.analytics.domain.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "name")
@ToString(of = "name")
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", length = 30, nullable = false, unique = true)
    private RoleName name;
}
