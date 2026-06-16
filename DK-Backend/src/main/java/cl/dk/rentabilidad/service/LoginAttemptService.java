package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protección contra fuerza bruta en el endpoint de login.
 *
 * <p>Bloquea temporalmente un email tras superar el máximo de intentos fallidos.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final long lockoutSeconds;
    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.security.login.max-attempts}") int maxAttempts,
            @Value("${app.security.login.lockout-minutes}") int lockoutMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockoutSeconds = lockoutMinutes * 60L;
    }

    /** Verifica si el email puede intentar autenticarse. */
    public void verificarPermitido(String email) {
        AttemptRecord record = attempts.get(normalizar(email));
        if (record != null && record.bloqueadoHasta != null
                && Instant.now().isBefore(record.bloqueadoHasta)) {
            throw new UnauthorizedException(
                    "Demasiados intentos fallidos. Intente nuevamente más tarde.");
        }
    }

    /** Registra un intento fallido. */
    public void registrarFallo(String email) {
        String key = normalizar(email);
        attempts.compute(key, (k, current) -> {
            AttemptRecord record = current != null ? current : new AttemptRecord();
            record.intentos++;
            if (record.intentos >= maxAttempts) {
                record.bloqueadoHasta = Instant.now().plusSeconds(lockoutSeconds);
            }
            return record;
        });
    }

    /** Limpia el contador tras un login exitoso. */
    public void registrarExito(String email) {
        attempts.remove(normalizar(email));
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static class AttemptRecord {
        private int intentos;
        private Instant bloqueadoHasta;
    }
}
