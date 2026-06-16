package cl.dk.rentabilidad.integration.falabella;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Las credenciales de Falabella. Salen de application.yml (sección falabella.api),
 * que a su vez las toma de variables de entorno o del .env local. Nunca van
 * hardcodeadas ni se suben al repo.
 */
@ConfigurationProperties(prefix = "falabella.api")
public record FalabellaProperties(
    String baseUrl,
    String userId,
    String apiKey,
    String sellerId,
    String version
) {
    /** Header User-Agent obligatorio: sin él, las requests fallan. */
    public String userAgent() {
        return sellerId + "/Java/21";
    }

    /** Para avisar con un error claro cuando alguien corre esto sin haber puesto el .env. */
    public boolean hasCredentials() {
        return userId != null && !userId.isBlank()
            && apiKey != null && !apiKey.isBlank();
    }
}
