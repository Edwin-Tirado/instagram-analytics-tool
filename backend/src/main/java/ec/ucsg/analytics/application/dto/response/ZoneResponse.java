package ec.ucsg.analytics.application.dto.response;

public record ZoneResponse(
    Long   id,
    String name,
    String description,
    Double latitude,
    Double longitude
) {}
