package ec.ucsg.analytics.application.dto.request;

import jakarta.validation.constraints.Size;

public record ApprovalRequest(

    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    String reason

) {}
