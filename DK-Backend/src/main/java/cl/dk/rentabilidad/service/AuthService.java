package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.config.JwtUtil;
import cl.dk.rentabilidad.dto.ChangePasswordRequest;
import cl.dk.rentabilidad.dto.LoginRequest;
import cl.dk.rentabilidad.dto.LoginResponse;
import cl.dk.rentabilidad.exception.ResourceNotFoundException;
import cl.dk.rentabilidad.entity.LogAcceso;
import cl.dk.rentabilidad.entity.Usuario;
import cl.dk.rentabilidad.exception.UnauthorizedException;
import cl.dk.rentabilidad.repository.LogAccesoRepository;
import cl.dk.rentabilidad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Autenticación de usuarios con protección contra fuerza bruta y auditoría de accesos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String MENSAJE_CREDENCIALES = "Credenciales inválidas";

    private final UsuarioRepository usuarioRepository;
    private final LogAccesoRepository logAccesoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipCliente) {
        String email = request.getEmail().trim().toLowerCase();
        loginAttemptService.verificarPermitido(email);

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())
                || !passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            loginAttemptService.registrarFallo(email);
            log.warn("Intento de login fallido para email={} ip={}", enmascararEmail(email), ipCliente);
            throw new UnauthorizedException(MENSAJE_CREDENCIALES);
        }

        loginAttemptService.registrarExito(email);

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        logAccesoRepository.save(LogAcceso.builder()
                .usuario(usuario)
                .accion("LOGIN")
                .ip(ipCliente)
                .build());

        log.info("Login exitoso para email={} ip={}", enmascararEmail(email), ipCliente);

        return LoginResponse.builder()
                .token(jwtUtil.generarToken(usuario.getEmail()))
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .build();
    }

    /**
     * Cambia la contraseña del usuario autenticado tras validar la contraseña actual.
     *
     * @param emailAutenticado email del JWT
     * @param request          contraseña actual, nueva y confirmación
     * @param ipCliente        IP para auditoría
     */
    @Transactional
    public void cambiarContrasena(String emailAutenticado, ChangePasswordRequest request, String ipCliente) {
        Usuario usuario = usuarioRepository.findByEmail(emailAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UnauthorizedException("Usuario inactivo");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), usuario.getPasswordHash())) {
            log.warn("Cambio de contraseña fallido (actual incorrecta) email={} ip={}",
                    enmascararEmail(emailAutenticado), ipCliente);
            throw new UnauthorizedException("La contraseña actual es incorrecta");
        }

        if (passwordEncoder.matches(request.getNewPassword(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta a la actual");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        usuarioRepository.save(usuario);

        logAccesoRepository.save(LogAcceso.builder()
                .usuario(usuario)
                .accion("CHANGE_PASSWORD")
                .ip(ipCliente)
                .build());

        log.info("Contraseña actualizada para email={} ip={}", enmascararEmail(emailAutenticado), ipCliente);
    }

    private String enmascararEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
