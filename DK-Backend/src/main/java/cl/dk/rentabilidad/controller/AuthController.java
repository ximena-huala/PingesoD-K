package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.config.SecurityContextHelper;
import cl.dk.rentabilidad.dto.ChangePasswordRequest;
import cl.dk.rentabilidad.dto.LoginRequest;
import cl.dk.rentabilidad.dto.LoginResponse;
import cl.dk.rentabilidad.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API de autenticación.
 */
@Tag(name = "Autenticación", description = "Login y obtención de token JWT")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Iniciar sesión", description = "Retorna un JWT. Máximo 5 intentos fallidos cada 15 minutos.")
    @ApiResponse(responseCode = "200", description = "Credenciales válidas")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas o cuenta bloqueada temporalmente")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, resolverIp(httpRequest)));
    }

    @Operation(
            summary = "Cambiar contraseña",
            description = "Requiere JWT. La nueva contraseña debe tener mínimo 8 caracteres, "
                    + "mayúscula, minúscula y número."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
    @ApiResponse(responseCode = "400", description = "Validación fallida o nueva contraseña igual a la actual")
    @ApiResponse(responseCode = "401", description = "Contraseña actual incorrecta o no autenticado")
    @PutMapping("/password")
    public ResponseEntity<Void> cambiarContrasena(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        String email = SecurityContextHelper.obtenerEmailAutenticado();
        authService.cambiarContrasena(email, request, resolverIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    private String resolverIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
