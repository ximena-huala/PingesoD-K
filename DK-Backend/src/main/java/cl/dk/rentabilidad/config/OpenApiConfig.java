package cl.dk.rentabilidad.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación interactiva de la API (OpenAPI 3 / Swagger UI).
 *
 * <p>Disponible en desarrollo en {@code /swagger-ui.html}.
 * La especificación JSON se expone en {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("D&K Integrador API")
                        .description("""
                                API REST para gestión de productos, canales de venta, \
                                costos operacionales, ventas y cálculo de rentabilidad.

                                Autenticación: obtener un JWT en POST /api/auth/login y \
                                enviarlo en el header Authorization: Bearer {token}.
                                """)
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("D&K")
                                .email("kevin@dk.cl")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido desde POST /api/auth/login")));
    }
}
