package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas del bloqueo por intentos fallidos ({@link LoginAttemptService}),
 * protección contra fuerza bruta en el login.
 */
class LoginAttemptServiceTest {

    /** 3 intentos permitidos, bloqueo de 15 minutos. */
    private LoginAttemptService service() {
        return new LoginAttemptService(3, 15);
    }

    @Test
    @DisplayName("Permite intentos por debajo del máximo")
    void permiteBajoDelMaximo() {
        LoginAttemptService s = service();
        s.registrarFallo("user@dk.cl");
        s.registrarFallo("user@dk.cl"); // 2 de 3
        assertDoesNotThrow(() -> s.verificarPermitido("user@dk.cl"));
    }

    @Test
    @DisplayName("Bloquea al alcanzar el máximo de intentos")
    void bloqueaEnElMaximo() {
        LoginAttemptService s = service();
        s.registrarFallo("user@dk.cl");
        s.registrarFallo("user@dk.cl");
        s.registrarFallo("user@dk.cl"); // 3 de 3 -> bloqueado
        assertThrows(UnauthorizedException.class, () -> s.verificarPermitido("user@dk.cl"));
    }

    @Test
    @DisplayName("Un login exitoso limpia el contador de intentos")
    void exitoLimpiaContador() {
        LoginAttemptService s = service();
        s.registrarFallo("user@dk.cl");
        s.registrarFallo("user@dk.cl");
        s.registrarExito("user@dk.cl");   // reset
        s.registrarFallo("user@dk.cl");   // vuelve a 1
        assertDoesNotThrow(() -> s.verificarPermitido("user@dk.cl"));
    }

    @Test
    @DisplayName("Normaliza el email: mayúsculas y espacios cuentan como el mismo usuario")
    void normalizaEmail() {
        LoginAttemptService s = service();
        s.registrarFallo("  ADMIN@DK.CL ");
        s.registrarFallo("admin@dk.cl");
        s.registrarFallo("Admin@Dk.Cl");   // 3 intentos, mismo email normalizado
        assertThrows(UnauthorizedException.class, () -> s.verificarPermitido("admin@dk.cl"));
    }
}
