package ec.ucsg.analytics.interfaces.rest;

import ec.ucsg.analytics.domain.enums.RoleName;
import ec.ucsg.analytics.domain.model.AppRole;
import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.repository.RoleRepository;
import ec.ucsg.analytics.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @GetMapping
    public Page<Map<String, Object>> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toMap);
    }

    @PatchMapping("/{id}/toggle-lock")
    public ResponseEntity<Map<String, Object>> toggleLock(@PathVariable UUID id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (user.isLocked()) user.unlock();
        else user.lock();
        userRepository.save(user);
        return ResponseEntity.ok(toMap(user));
    }

    @PatchMapping("/{id}/toggle-enabled")
    public ResponseEntity<Map<String, Object>> toggleEnabled(@PathVariable UUID id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return ResponseEntity.ok(toMap(user));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<Map<String, Object>> changeRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Los admins no pueden ser degradados desde aquí
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (isAdmin) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "No se puede cambiar el rol de un administrador"));
        }

        String newRoleName = body.get("role");
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(newRoleName);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Rol inválido: " + newRoleName));
        }

        AppRole newRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado: " + roleName));

        user.getRoles().clear();
        user.getRoles().add(newRole);
        userRepository.save(user);

        return ResponseEntity.ok(toMap(user));
    }

    private Map<String, Object> toMap(AppUser u) {
        Set<String> roles = u.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
        return Map.of(
                "id",                  u.getId(),
                "email",               u.getEmail(),
                "fullName",            u.getFullName() != null ? u.getFullName() : "",
                "enabled",             u.isEnabled(),
                "locked",              u.isLocked(),
                "failedLoginAttempts", u.getFailedLoginAttempts(),
                "createdAt",           u.getCreatedAt() != null ? u.getCreatedAt().toString() : "",
                "roles",               roles
        );
    }
}
