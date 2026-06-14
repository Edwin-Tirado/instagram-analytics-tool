package ec.ucsg.analytics.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "email", "enabled"})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Email
    // Dominio estrictamente universitario — regla de negocio crítica
    @Pattern(
        regexp = "^[a-zA-Z0-9._%+\\-]+@cu\\.ucsg\\.edu\\.ec$",
        message = "Solo se permiten correos con dominio @cu.ucsg.edu.ec"
    )
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", length = 120)
    private String fullName;

    // ── RBAC ───────────────────────────────────────────────────────
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<AppRole> roles = new HashSet<>();

    // ── Control de intentos fallidos y bloqueo ─────────────────────
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked", nullable = false)
    @Builder.Default
    private boolean locked = false;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    // ── Estado de la cuenta ─────────────────────────────────────────
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // ── Auditoría ───────────────────────────────────────────────────
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Métodos de dominio ──────────────────────────────────────────

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }

    public void lock() {
        this.locked = true;
        this.lockTime = LocalDateTime.now();
    }

    public void unlock() {
        this.locked = false;
        this.lockTime = null;
        this.failedLoginAttempts = 0;
    }
}
