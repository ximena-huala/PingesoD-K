package cl.dk.rentabilidad.integration.falabella;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Firma las llamadas a Falabella. Cada request lleva un parámetro Signature que
 * es un HMAC-SHA256 del resto de los parámetros, usando la API Key como secreto.
 * Ojo: la API Key nunca se manda en la request, solo sirve para firmar acá.
 *
 * La receta, en este orden exacto (si te equivocas en un paso Falabella tira
 * E007): juntar los parámetros menos Signature, ordenarlos por nombre,
 * url-encodear cada "clave=valor", pegarlos con & y sacarle el HMAC en hex
 * minúscula. Lo dejamos validado contra la API real con la cuenta del seller.
 */
@Service
public class HmacSignatureService {

    private static final DateTimeFormatter ISO_8601 =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public String sign(Map<String, String> params, String apiKey) {
        try {
            String stringToSign = buildStringToSign(params);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return bytesToHex(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Error generando firma HMAC-SHA256", e);
        }
    }

    /**
     * El texto exacto sobre el que se calcula la firma. Lo dejamos público a
     * propósito: el cliente arma la URL con este mismísimo string, porque si la
     * query que viaja no calza carácter por carácter con lo que firmamos,
     * Falabella responde E007.
     */
    public String buildStringToSign(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
            .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
            .collect(Collectors.joining("&"));
    }

    /** Timestamp ISO 8601 con timezone local, ej: 2026-05-15T14:30:00-04:00 */
    public String generateTimestamp() {
        return OffsetDateTime.now().format(ISO_8601);
    }

    public String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
