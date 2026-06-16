package cl.dk.rentabilidad.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Solicitud de cambio de contraseña para un usuario autenticado.
 */
@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "La contraseña actual es requerida")
    private String currentPassword;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 8, max = 128, message = "La nueva contraseña debe tener entre 8 y 128 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La nueva contraseña debe incluir al menos una mayúscula, una minúscula y un número"
    )
    private String newPassword;

    @NotBlank(message = "La confirmación de contraseña es requerida")
    private String confirmNewPassword;

    @AssertTrue(message = "La confirmación no coincide con la nueva contraseña")
    public boolean isConfirmacionValida() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }
}
