package ec.ucsg.analytics.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    String password

) {}
