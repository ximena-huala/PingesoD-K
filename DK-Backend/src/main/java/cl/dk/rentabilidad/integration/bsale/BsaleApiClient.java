package cl.dk.rentabilidad.integration.bsale;

import cl.dk.rentabilidad.integration.bsale.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP para la API REST de Bsale (Chile).
 *
 * @see <a href="https://apichile.bsalelab.com/">Documentación API Bsale</a>
 */
@Slf4j
@Component
public class BsaleApiClient {

    private final BsaleProperties properties;
    private volatile RestClient restClient;

    public BsaleApiClient(BsaleProperties properties) {
        this.properties = properties;
    }

    public int contarVariantesActivas() {
        BsaleCountDto response = client().get()
                .uri("/v1/variants/count.json?state=0")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new BsaleApiException("Error al contar variantes Bsale: HTTP " + res.getStatusCode());
                })
                .body(BsaleCountDto.class);
        return response != null && response.getCount() != null ? response.getCount() : 0;
    }

    public BsalePagedResponse<BsaleVariantDto> listarVariantesActivas(int offset) {
        return client().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/variants.json")
                        .queryParam("state", 0)
                        .queryParam("limit", properties.getPageSize())
                        .queryParam("offset", offset)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new BsaleApiException("Error al listar variantes Bsale: HTTP " + res.getStatusCode());
                })
                .body(new ParameterizedTypeReference<>() {});
    }

    public BsaleVariantCostDto obtenerCosto(int variantId) {
        return client().get()
                .uri("/v1/variants/{id}/costs.json", variantId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new BsaleApiException(
                            "Error al obtener costo de variante " + variantId + ": HTTP " + res.getStatusCode());
                })
                .body(BsaleVariantCostDto.class);
    }

    public BsaleProductDto obtenerProducto(int productId) {
        return client().get()
                .uri("/v1/products/{id}.json", productId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new BsaleApiException(
                            "Error al obtener producto " + productId + ": HTTP " + res.getStatusCode());
                })
                .body(BsaleProductDto.class);
    }

    private RestClient client() {
        validarConfiguracion();
        if (restClient == null) {
            synchronized (this) {
                if (restClient == null) {
                    restClient = RestClient.builder()
                            .baseUrl(properties.getBaseUrl())
                            .defaultHeader("access_token", properties.getAccessToken())
                            .defaultHeader("Accept", "application/json")
                            .build();
                }
            }
        }
        return restClient;
    }

    private void validarConfiguracion() {
        if (!properties.isEnabled()) {
            throw new BsaleApiException("La integración Bsale está deshabilitada (app.integrations.bsale.enabled=false)");
        }
        if (properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            throw new BsaleApiException("BSALE_ACCESS_TOKEN no está configurado");
        }
    }

    public static class BsaleApiException extends RuntimeException {
        public BsaleApiException(String message) {
            super(message);
        }
    }
}
