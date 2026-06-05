package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.dto.LoginRequest;
import cl.dk.rentabilidad.dto.LoginResponse;
import cl.dk.rentabilidad.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación.
 * Expone el endpoint de login que valida credenciales
 * y retorna un token JWT para las siguientes peticiones.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Autentica un usuario y retorna su token JWT.
     * El token debe enviarse en el header Authorization: Bearer {token}
     * en todas las peticiones posteriores.
     *
     * @param request body con email y password
     * @return token JWT + datos básicos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
