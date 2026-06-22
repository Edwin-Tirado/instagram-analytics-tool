package ec.ucsg.analytics.application.service;

/**
 * Resultado del análisis heurístico de un caption de Instagram.
 *
 * EVENT        → señales de evento detectadas → ingesta como PENDING
 * AUTO_REJECTED → señales de spam/publicidad  → persistir como REJECTED y no mostrar al supervisor
 * SKIP         → sin señales claras           → descartar silenciosamente (no ingestar)
 */
public enum ClassificationResult {
    EVENT,
    AUTO_REJECTED,
    SKIP
}
