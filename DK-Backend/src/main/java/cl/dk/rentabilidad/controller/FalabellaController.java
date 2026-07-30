package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.dto.FalabellaEstadoDto;
import cl.dk.rentabilidad.integration.falabella.FalabellaClient;
import cl.dk.rentabilidad.integration.falabella.FalabellaProperties;
import cl.dk.rentabilidad.integration.falabella.FalabellaSyncService;
import cl.dk.rentabilidad.integration.falabella.dto.FalabellaSyncResult;
import cl.dk.rentabilidad.repository.VentaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * Integración con Falabella para el frontend (autenticada con JWT).
 *
 * A diferencia de {@code /api/dev/falabella/*} (que es solo para pruebas en perfil
 * dev y sin token), estos endpoints son los que consume la app:
 *   - /estado: si hay credenciales, si la conexión responde y cuántas ventas hay.
 *   - /sync:   dispara la sincronización de ventas desde la API de Falabella.
 */
@Tag(name = "Integración Falabella", description = "Estado y sincronización del canal Falabella")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/integraciones/falabella")
@RequiredArgsConstructor
public class FalabellaController {

    private static final String CANAL = "Falabella";

    private final FalabellaClient client;
    private final FalabellaSyncService syncService;
    private final FalabellaProperties props;
    private final VentaRepository ventaRepository;

    @Operation(summary = "Estado de la integración",
            description = "Credenciales, conexión en vivo con Falabella, y ventas cargadas del canal")
    @GetMapping("/estado")
    public ResponseEntity<FalabellaEstadoDto> estado() {
        boolean credenciales = props.hasCredentials();
        boolean conexionOk = false;
        String mensaje;

        if (!credenciales) {
            mensaje = "Faltan credenciales en el .env (FALABELLA_USER_ID / FALABELLA_API_KEY)";
        } else {
            try {
                // Llamada liviana solo para confirmar que la firma y la conexión responden.
                client.call("GetOrders", Map.of("Limit", "1"));
                conexionOk = true;
                mensaje = "Conexión con Falabella OK";
            } catch (Exception e) {
                mensaje = "No se pudo conectar con Falabella: " + e.getMessage();
            }
        }

        return ResponseEntity.ok(new FalabellaEstadoDto(
                CANAL,
                props.sellerId(),
                credenciales,
                conexionOk,
                mensaje,
                ventaRepository.contarPorCanal(CANAL),
                ventaRepository.ultimaFechaPorCanal(CANAL)));
    }

    @Operation(summary = "Sincronizar ventas desde Falabella",
            description = "Trae las órdenes entregadas desde la fecha indicada y las guarda como ventas. "
                    + "Es un proceso lento (varios minutos) por las pausas anti rate-limit. "
                    + "Si no se indica fecha, toma los últimos 3 meses.")
    @PostMapping("/sync")
    public ResponseEntity<FalabellaSyncResult> sync(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde) {
        LocalDate inicio = desde != null ? desde : LocalDate.now().minusMonths(3);
        return ResponseEntity.ok(syncService.sincronizarVentas(inicio));
    }

    // ── Explorador: consultas de lectura a la API de Falabella, en vivo ──────────
    // Devuelven el JSON crudo que responde Falabella. Sirven para demostrar en el
    // frontend que la integración está viva. Todo es de solo lectura (Get*).

    @Operation(summary = "En vivo: últimas órdenes de Falabella")
    @GetMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> orders(@RequestParam(required = false) String createdAfter) {
        return ResponseEntity.ok(client.getOrders(createdAfter));
    }

    @Operation(summary = "En vivo: catálogo de productos de Falabella")
    @GetMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> products(@RequestParam(defaultValue = "5") int limit,
                                           @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(client.getProducts(limit, offset));
    }

    @Operation(summary = "En vivo: árbol de categorías de Falabella")
    @GetMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> categories() {
        return ResponseEntity.ok(client.getCategoryTree());
    }

    @Operation(summary = "En vivo: marcas de Falabella")
    @GetMapping(value = "/brands", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> brands() {
        return ResponseEntity.ok(client.getBrands());
    }

    @Operation(summary = "En vivo: items de una orden de Falabella")
    @GetMapping(value = "/order-items/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> orderItems(@PathVariable String orderId) {
        return ResponseEntity.ok(client.getOrderItems(orderId));
    }

    @Operation(summary = "Prueba de firma inválida: Falabella debe rechazarla con E007",
            description = "Manda una request con la firma adulterada a propósito. Demuestra que la "
                    + "API valida la autenticación (no es un mock local). Devuelve el error de Falabella.")
    @GetMapping(value = "/test-firma", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> testFirma() {
        return ResponseEntity.ok(client.callConFirmaInvalida("GetOrders"));
    }
}
