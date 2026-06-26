package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.service.RentabilidadService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de prueba para recalcular la rentabilidad sin reimportar ventas.
 * Útil después de tocar costo_canal: vuelve a correr el motor sobre lo que ya
 * está en la base. Solo existe en el perfil dev.
 */
@RestController
@RequestMapping("/api/dev/rentabilidad")
@Profile("dev")
public class RentabilidadDevController {

    private final RentabilidadService rentabilidadService;

    public RentabilidadDevController(RentabilidadService rentabilidadService) {
        this.rentabilidadService = rentabilidadService;
    }

    @PostMapping("/recalcular")
    public ResponseEntity<Map<String, Object>> recalcular() {
        int n = rentabilidadService.recalcularTodas();
        return ResponseEntity.ok(Map.of("ventasRecalculadas", n));
    }
}
