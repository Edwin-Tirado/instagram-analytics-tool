package ec.ucsg.analytics.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload del endpoint POST /api/auth/register.
 */
public record RegisterRequest(

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    String fullName,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_\\-])[A-Za-z\\d@$!%*?&_\\-]{8,}$",
        message = "La contraseña debe contener al menos: una mayúscula, una minúscula, un número y un carácter especial"
    )
    String password

) {}

