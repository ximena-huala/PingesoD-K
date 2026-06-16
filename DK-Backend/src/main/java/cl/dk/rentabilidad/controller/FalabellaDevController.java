package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.integration.falabella.FalabellaClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints para probar a mano que la integración con Falabella funciona,
 * pegándole desde el navegador o Postman sin escribir código.
 *
 * Vive solo en el perfil dev: si la app corre sin "dev", este controller ni se
 * crea, así que no hay que acordarse de borrarlo antes de producción.
 * SecurityConfig deja pasar /api/dev/** sin token, también solo en dev.
 */
@RestController
@RequestMapping("/api/dev/falabella")
@Profile("dev")
public class FalabellaDevController {

    private final FalabellaClient client;

    public FalabellaDevController(FalabellaClient client) {
        this.client = client;
    }

    @GetMapping("/metrics")
    public ResponseEntity<String> metrics() {
        return ResponseEntity.ok(client.getMetrics("alltime"));
    }

    @GetMapping("/categories")
    public ResponseEntity<String> categories() {
        return ResponseEntity.ok(client.getCategoryTree());
    }

    @GetMapping("/brands")
    public ResponseEntity<String> brands() {
        return ResponseEntity.ok(client.getBrands());
    }

    @GetMapping("/products")
    public ResponseEntity<String> products(@RequestParam(defaultValue = "10") int limit,
                                           @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(client.getProducts(limit, offset));
    }

    @GetMapping("/orders")
    public ResponseEntity<String> orders(@RequestParam(required = false) String createdAfter) {
        return ResponseEntity.ok(client.getOrders(createdAfter));
    }

    @GetMapping("/order-items/{orderId}")
    public ResponseEntity<String> orderItems(@PathVariable String orderId) {
        return ResponseEntity.ok(client.getOrderItems(orderId));
    }
}
