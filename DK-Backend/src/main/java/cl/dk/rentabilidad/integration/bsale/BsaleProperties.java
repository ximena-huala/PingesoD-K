package cl.dk.rentabilidad.integration.bsale;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de la integración con Bsale.
 *
 * <p>Token: solicitar a ayuda@bsale.app o desde el panel de Bsale de la empresa.
 * Variable de entorno: {@code BSALE_ACCESS_TOKEN}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.integrations.bsale")
public class BsaleProperties {

    /** Activa o desactiva la integración. */
    private boolean enabled = false;

    /** URL base de la API (Chile: https://api.bsale.cl). */
    private String baseUrl = "https://api.bsale.cl";

    /** Token de acceso enviado en el header {@code access_token}. */
    private String accessToken = "";

    /** Variantes por página (máximo permitido por Bsale: 50). */
    private int pageSize = 50;
}
