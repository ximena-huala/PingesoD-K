package cl.dk.rentabilidad.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del token JWT cargadas desde configuración o variables de entorno.
 *
 * <p>En producción {@code JWT_SECRET} es obligatorio y debe tener al menos 32 caracteres.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** Clave HMAC para firmar tokens (variable de entorno: JWT_SECRET). */
    private String secret;

    /** Duración del token en milisegundos (por defecto 8 horas). */
    private long expiration = 28_800_000L;
}
