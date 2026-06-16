package cl.dk.rentabilidad.integration.falabella;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ppara confirmar que el backend logra conectarse. Usa las mismas clases que la app (firma + cliente + parser) pero
 * sin levantar Spring ni la BD, así uno prueba la conexión sin tener que montar
 * PostgreSQL.
 *
 * Para correrlo:
 *   ./gradlew test --tests "*FalabellaConnectivitySmokeTest*"
 *
 * Si no hay credenciales (en el .env o en variables de entorno) el test se salta
 * en vez de fallar, para no romperle el build a alguien que todavía no las
 * tiene cargadas.
 */
class FalabellaConnectivitySmokeTest {

    private static FalabellaProperties props;

    @BeforeAll
    static void cargarCredenciales() throws IOException {
        // Primero las variables de entorno y, si no están, leemos el .env
        // (Gradle corre los tests parado en DK-Backend, así la ruta relativa calza).
        Map<String, String> vars = new HashMap<>(System.getenv());
        Path env = Path.of(".env");
        if (Files.exists(env)) {
            for (String line : Files.readAllLines(env)) {
                int eq = line.indexOf('=');
                if (eq > 0 && !line.trim().startsWith("#")) {
                    vars.putIfAbsent(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        }
        props = new FalabellaProperties(
            "https://sellercenter-api.falabella.com",
            vars.getOrDefault("FALABELLA_USER_ID", ""),
            vars.getOrDefault("FALABELLA_API_KEY", ""),
            vars.getOrDefault("FALABELLA_SELLER_ID", ""),
            "1.0");
    }

    @Test
    void elBackendConectaConLaApiDeFalabella() {
        assumeTrue(props.hasCredentials(),
            "Sin credenciales Falabella (.env) — smoke test omitido");

        FalabellaClient client = new FalabellaClient(
            new HmacSignatureService(), new FalabellaResponseParser(), props);

        // Una orden real que sabemos que existe; solo lectura.
        String body = client.getOrderItems("1139186719");

        assertTrue(body.contains("\"OrderItems\""), "El Body debe traer OrderItems");
        assertTrue(body.contains("\"PaidPrice\":\"50992.00\""),
            "La orden de referencia debe traer PaidPrice=50992.00");
        System.out.println("✅ Backend conectado a Falabella: GetOrderItems respondió "
            + body.length() + " chars con los valores esperados");
    }
}
