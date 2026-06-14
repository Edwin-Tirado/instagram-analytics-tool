package ec.ucsg.analytics.domain.enums;

/**
 * Ciclo de vida de un evento extraído desde Instagram.
 * PENDING  : recién ingestado — pendiente de revisión del supervisor.
 * APPROVED : aprobado para difusión en el mapa.
 * REJECTED : descartado por el supervisor.
 */
public enum EventStatus {
    PENDING,
    APPROVED,
    REJECTED
}
