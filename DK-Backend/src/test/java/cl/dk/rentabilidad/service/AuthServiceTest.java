package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.config.JwtUtil;
import cl.dk.rentabilidad.dto.LoginRequest;
import cl.dk.rentabilidad.dto.LoginResponse;
import cl.dk.rentabilidad.entity.LogAcceso;
import cl.dk.rentabilidad.entity.Usuario;
import cl.dk.rentabilidad.exception.UnauthorizedException;
import cl.dk.rentabilidad.repository.LogAccesoRepository;
import cl.dk.rentabilidad.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del flujo de autenticación ({@link AuthService}): login exitoso,
 * credenciales inválidas, usuario inactivo/inexistente y bloqueo por intentos.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock LogAccesoRepository logAccesoRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock LoginAttemptService loginAttemptService;

    @InjectMocks AuthService service;

    @Test
    @DisplayName("Login exitoso devuelve token y datos del usuario")
    void loginExitoso() {
        when(usuarioRepository.findByEmail("kevin@dk.cl")).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches("changeme", "$2a$hash")).thenReturn(true);
        when(jwtUtil.generarToken("kevin@dk.cl")).thenReturn("token-jwt");

        // El email entra con mayúsculas/espacios: debe normalizarse.
        LoginResponse resp = service.login(req(" Kevin@DK.cl ", "changeme"), "1.2.3.4");

        assertEquals("token-jwt", resp.getToken());
        assertEquals("kevin@dk.cl", resp.getEmail());
        assertEquals("Kevin", resp.getNombre());
        verify(loginAttemptService).registrarExito("kevin@dk.cl");
        verify(logAccesoRepository).save(any(LogAcceso.class));   // auditoría
    }

    @Test
    @DisplayName("Contraseña incorrecta lanza UnauthorizedException y registra el fallo")
    void passwordIncorrecta() {
        when(usuarioRepository.findByEmail("kevin@dk.cl")).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> service.login(req("kevin@dk.cl", "mala"), "1.2.3.4"));

        verify(loginAttemptService).registrarFallo("kevin@dk.cl");
        verify(jwtUtil, never()).generarToken(any());   // no emite token
    }

    @Test
    @DisplayName("Usuario inexistente lanza UnauthorizedException")
    void usuarioNoExiste() {
        when(usuarioRepository.findByEmail("nadie@dk.cl")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> service.login(req("nadie@dk.cl", "x"), "1.2.3.4"));

        verify(loginAttemptService).registrarFallo("nadie@dk.cl");
    }

    @Test
    @DisplayName("Usuario inactivo no puede iniciar sesión")
    void usuarioInactivo() {
        when(usuarioRepository.findByEmail("kevin@dk.cl")).thenReturn(Optional.of(usuario(false)));

        assertThrows(UnauthorizedException.class,
                () -> service.login(req("kevin@dk.cl", "changeme"), "1.2.3.4"));

        verify(loginAttemptService).registrarFallo("kevin@dk.cl");
    }

    @Test
    @DisplayName("Si el email está bloqueado, ni siquiera consulta al usuario")
    void bloqueadoNoConsultaUsuario() {
        doThrow(new UnauthorizedException("Demasiados intentos"))
                .when(loginAttemptService).verificarPermitido("kevin@dk.cl");

        assertThrows(UnauthorizedException.class,
                () -> service.login(req("kevin@dk.cl", "changeme"), "1.2.3.4"));

        verify(usuarioRepository, never()).findByEmail(any());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private LoginRequest req(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private Usuario usuario(boolean activo) {
        return Usuario.builder()
                .nombre("Kevin")
                .email("kevin@dk.cl")
                .passwordHash("$2a$hash")
                .activo(activo)
                .build();
    }
}
