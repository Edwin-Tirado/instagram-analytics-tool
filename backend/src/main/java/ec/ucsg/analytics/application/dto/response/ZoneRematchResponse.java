package ec.ucsg.analytics.application.dto.response;

/** Resultado de re-emparejar eventos sin zona contra el caption ya guardado. */
public record ZoneRematchResponse(int matched, int candidates) {}
