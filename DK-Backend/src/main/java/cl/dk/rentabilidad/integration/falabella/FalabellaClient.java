package cl.dk.rentabilidad.integration.falabella;

import cl.dk.rentabilidad.integration.falabella.exception.FalabellaApiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Cliente HTTP que habla con Falabella: arma la request, la firma y devuelve
 * el Body ya desenvuelto (o lanza FalabellaApiException si vino un error).
 *
 * El detalle delicado está en cómo mandamos la URL. Construimos el
 * java.net.URI a mano en lugar de pasarle los parámetros sueltos a Spring,
 * porque Spring los re-encodea y entonces la query que viaja deja de calzar
 * con lo que firmamos → Falabella responde E007.
 *
 * Acá solo hay métodos de lectura (Get*). No agregar Set/Create/Update/Delete:
 * Falabella no tiene ambiente de pruebas, así que cualquier escritura toca los
 * datos reales del cliente.
 */
@Component
public class FalabellaClient {

    private final RestClient restClient;
    private final HmacSignatureService signatureService;
    private final FalabellaResponseParser parser;
    private final FalabellaProperties props;

    public FalabellaClient(HmacSignatureService signatureService,
                           FalabellaResponseParser parser,
                           FalabellaProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.signatureService = signatureService;
        this.parser = parser;
        this.props = props;
    }

    /**
     * Arma, firma y dispara un GET. Devuelve el Body de la respuesta como JSON
     * crudo, o lanza FalabellaApiException si faltan credenciales o si Falabella
     * contestó con error.
     */
    public String call(String action, Map<String, String> extraParams) {
        if (!props.hasCredentials()) {
            throw new FalabellaApiException("NO_CREDENTIALS",
                "Faltan FALABELLA_USER_ID / FALABELLA_API_KEY (ver .env.example)", action);
        }

        Map<String, String> params = new HashMap<>();
        params.put("Action", action);
        params.put("Format", "JSON");
        params.put("Timestamp", signatureService.generateTimestamp());
        params.put("UserID", props.userId());
        params.put("Version", props.version());
        if (extraParams != null) params.putAll(extraParams);

        params.put("Signature", signatureService.sign(params, props.apiKey()));

        // Misma codificación que usamos para firmar. La dejamos ordenada igual
        // que la firma (a Falabella no le importa el orden, pero así es más
        // fácil comparar a ojo cuando algo no calza).
        String queryString = new TreeMap<>(params).entrySet().stream()
            .map(e -> signatureService.urlEncode(e.getKey()) + "="
                    + signatureService.urlEncode(e.getValue()))
            .collect(Collectors.joining("&"));

        URI uri = URI.create(props.baseUrl() + "/?" + queryString);

        String rawBody = restClient.get()
            .uri(uri)
            .header(HttpHeaders.USER_AGENT, props.userAgent())
            .retrieve()
            .body(String.class);

        return parser.unwrapOrThrow(rawBody, action);
    }

    // ---- Atajos para los endpoints que usamos (todos de lectura) ----

    public String getMetrics(String type) {
        return call("GetMetrics", Map.of("StatisticsType", type));
    }

    public String getCategoryTree() {
        return call("GetCategoryTree", null);
    }

    public String getBrands() {
        return call("GetBrands", null);
    }

    public String getOrders(String createdAfter) {
        return call("GetOrders", createdAfter != null ? Map.of("CreatedAfter", createdAfter) : null);
    }

    public String getOrder(String orderId) {
        return call("GetOrder", Map.of("OrderId", orderId));
    }

    public String getOrderItems(String orderId) {
        return call("GetOrderItems", Map.of("OrderId", orderId));
    }

    public String getMultipleOrderItems(String orderIdList) {
        return call("GetMultipleOrderItems", Map.of("OrderIdList", orderIdList));
    }

    public String getProducts(int limit, int offset) {
        return call("GetProducts", Map.of(
            "Limit", String.valueOf(limit),
            "Offset", String.valueOf(offset)));
    }
}
