package cl.dk.rentabilidad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO que representa la respuesta exitosa del login.
 * Contiene el token JWT y datos básicos del usuario autenticado.
 */
@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    /** Token JWT para autenticar las siguientes peticiones */
    private String token;

    /** Nombre del usuario autenticado (para mostrar en el frontend) */
    private String nombre;

    /** Email del usuario autenticado */
    private String email;
}