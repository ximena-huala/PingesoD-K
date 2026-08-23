package cl.dk.rentabilidad.integration.falabella;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de la firma HMAC. Son puro Java: no levantan Spring ni la BD, así que
 * corren en milisegundos.
 */
class HmacSignatureServiceTest {

    private final HmacSignatureService service = new HmacSignatureService();

    @Test
    void firmaConsistenteParaMismoInput() {
        Map<String, String> params = Map.of(
            "Action", "GetOrders",
            "Format", "JSON",
            "Timestamp", "2026-05-15T14:30:00-04:00",
            "UserID", "seller@dk.cl",
            "Version", "1.0");
        String s1 = service.sign(params, "test-key");
        String s2 = service.sign(params, "test-key");
        assertEquals(s1, s2);
        assertEquals(64, s1.length());
        assertTrue(s1.matches("[0-9a-f]{64}"), "La firma debe ser hex lowercase");
    }

    @Test
    void ordenDeInsercionNoAfectaLaFirma() {
        Map<String, String> p1 = new LinkedHashMap<>();
        p1.put("Version", "1.0");
        p1.put("Action", "GetOrders");
        p1.put("Format", "JSON");
        Map<String, String> p2 = new LinkedHashMap<>();
        p2.put("Action", "GetOrders");
        p2.put("Format", "JSON");
        p2.put("Version", "1.0");
        assertEquals(service.sign(p1, "k"), service.sign(p2, "k"));
    }

    @Test
    void timestampTieneFormatoIso8601ConTimezone() {
        // El offset puede ser numérico (ej. -04:00, cuando el server corre en
        // horario de Chile) o "Z" (cuando corre en UTC, como el contenedor de la
        // VM o el runner de CI). Ambos son ISO 8601 válidos y Falabella los acepta.
        assertTrue(service.generateTimestamp()
            .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([+-]\\d{2}:\\d{2}|Z)"));
    }

    @Test
    void stringToSignCodificaCaracteresEspeciales() {
        // Acá se cae mucha gente: los ':' del timestamp tienen que quedar como
        // %3A y el '@' del email como %40. Si el encoding no calza → E007.
        Map<String, String> params = Map.of(
            "Timestamp", "2026-05-15T14:30:00-04:00",
            "UserID", "seller@dk.cl");
        String toSign = service.buildStringToSign(params);
        assertEquals("Timestamp=2026-05-15T14%3A30%3A00-04%3A00&UserID=seller%40dk.cl", toSign);
    }

    @Test
    void firmasDistintasConApiKeysDistintas() {
        Map<String, String> params = Map.of("Action", "GetMetrics");
        assertTrue(!service.sign(params, "key-1").equals(service.sign(params, "key-2")));
    }
}
