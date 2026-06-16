package cl.dk.rentabilidad.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Valida en arranque que la configuración JWT sea segura en producción.
 */
@Component
@Profile("prod")
public class JwtSecretValidator {

    private final JwtProperties jwtProperties;

    public JwtSecretValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void validar() {
        String secret = jwtProperties.getSecret();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET es obligatorio en producción (mínimo 32 caracteres aleatorios)");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        if (secret.contains("dev-only") || secret.contains("changeme") || secret.contains("muy-segura")) {
            throw new IllegalStateException("JWT_SECRET no puede usar valores por defecto en producción");
        }
    }
}
