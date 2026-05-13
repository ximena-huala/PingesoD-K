package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.config.JwtUtil;
import cl.dk.rentabilidad.dto.LoginRequest;
import cl.dk.rentabilidad.dto.LoginResponse;
import cl.dk.rentabilidad.entity.Usuario;
import cl.dk.rentabilidad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Servicio de autenticación.
 *
 * Valida las credenciales del usuario contra la base de datos
 * y genera un token JWT para las siguientes peticiones.
 *
 * Los 3 usuarios del sistema (Kevin, Daniel, Arnely) tienen
 * todos nivel administrador según lo acordado con el cliente.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Autentica un usuario y retorna su token JWT.
     *
     * @param request DTO con email y contraseña
     * @return DTO con token JWT y datos del usuario
     * @throws RuntimeException si las credenciales son inválidas
     */
    public LoginResponse login(LoginRequest request) {

        // 1. Buscamos el usuario por email
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        // 2. Verificamos que el usuario esté activo
        if (!usuario.getActivo()) {
            throw new RuntimeException("Usuario inactivo");
        }

        // 3. Comparamos la contraseña ingresada contra el hash en BD
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        // 4. Registramos el último acceso del usuario
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // 5. Generamos el token JWT con el email como identificador
        String token = jwtUtil.generarToken(usuario.getEmail());

        return LoginResponse.builder()
                .token(token)
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .build();
    }
}