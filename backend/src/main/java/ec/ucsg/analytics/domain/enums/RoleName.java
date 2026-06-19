package ec.ucsg.analytics.domain.enums;

/**
 * Roles del sistema — RBAC.
 * ADMIN    : acceso total + auditoría.
 * SUPERVISOR: revisa posts PENDING, aprueba o rechaza.
 * USER     : consume el mapa y gestiona recordatorios.
 */

public enum RoleName {
    ROLE_ADMIN,
    ROLE_SUPERVISOR,
    ROLE_USER
}
