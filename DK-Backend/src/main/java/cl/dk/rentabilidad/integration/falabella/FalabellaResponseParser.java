package cl.dk.rentabilidad.integration.falabella;

import cl.dk.rentabilidad.integration.falabella.exception.FalabellaApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Falabella siempre envuelve la respuesta: si salió bien viene un
 * "SuccessResponse" con un Body adentro, y si salió mal un "ErrorResponse" con
 * su ErrorCode. Acá miramos cuál de los dos llegó y devolvemos el Body o
 * lanzamos la excepción con el código.
 */
@Component
public class FalabellaResponseParser {

    private final ObjectMapper mapper = new ObjectMapper();

    /** Devuelve el Body si todo salió bien; si no, lanza la excepción con el código de error. */
    public String unwrapOrThrow(String rawBody, String action) {
        try {
            JsonNode root = mapper.readTree(rawBody);

            if (root.has("ErrorResponse")) {
                JsonNode head = root.path("ErrorResponse").path("Head");
                throw new FalabellaApiException(
                    head.path("ErrorCode").asText("UNKNOWN"),
                    head.path("ErrorMessage").asText("Sin mensaje"),
                    action
                );
            }
            if (root.has("SuccessResponse")) {
                return root.path("SuccessResponse").path("Body").toString();
            }
            throw new FalabellaApiException("MALFORMED", "Respuesta sin envelope conocido", action);
        } catch (FalabellaApiException e) {
            throw e;
        } catch (Exception e) {
            throw new FalabellaApiException("PARSE_ERROR", e.getMessage(), action);
        }
    }
}
