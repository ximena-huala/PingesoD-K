package cl.dk.rentabilidad.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilidad para generar y validar tokens JWT.
 *
 * El token contiene el email del usuario como subject y expira
 * según el tiempo configurado en application.yml.
 */
@Component
public class JwtUtil {

    /** Clave secreta para firmar los tokens, definida en application.yml */
    @Value("${jwt.secret}")
    private String secret;

    /** Tiempo de expiración en milisegundos (ej: 86400000 = 24 horas) */
    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Genera un token JWT para el email dado.
     *
     * @param email identificador único del usuario
     * @return token JWT firmado
     */
    public String generarToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    /**
     * Extrae el email del subject del token JWT.
     *
     * @param token JWT a procesar
     * @return email contenido en el token
     */
    public String extraerEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Verifica si el token es válido y no ha expirado.
     *
     * @param token JWT a validar
     * @return true si el token es válido
     */
    public boolean esValido(String token) {
        try {
            return getClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            // Token inválido, malformado o expirado
            return false;
        }
    }

    /**
     * Obtiene la clave secreta como objeto SecretKey para firmar/validar.
     */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parsea el token y extrae todos sus claims (datos internos).
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}