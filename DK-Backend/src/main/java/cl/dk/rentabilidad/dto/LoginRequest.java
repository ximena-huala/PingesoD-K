package cl.dk.rentabilidad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO que representa los datos de entrada para el login.
 * Se valida automáticamente con @Valid en el controller.
 */
@Getter
@Setter
public class LoginRequest {

    /** Email del usuario registrado en el sistema */
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    /** Contraseña en texto plano (se compara contra el hash en BD) */
    @NotBlank(message = "La contraseña es requerida")
    private String password;
}