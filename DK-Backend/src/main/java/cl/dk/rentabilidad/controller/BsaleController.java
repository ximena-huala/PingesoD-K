package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.entity.IntegracionSyncLog;
import cl.dk.rentabilidad.integration.bsale.BsaleProductSyncService;
import cl.dk.rentabilidad.integration.bsale.dto.BsaleSyncResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de integración con Bsale (catálogo maestro: SKU + costo).
 */
@Tag(name = "Integración Bsale", description = "Sincronización de productos desde Bsale")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/integraciones/bsale")
@RequiredArgsConstructor
public class BsaleController {

    private final BsaleProductSyncService bsaleProductSyncService;

    @Operation(
            summary = "Sincronizar productos desde Bsale",
            description = "Importa/actualiza SKU y costo_base desde variantes activas de Bsale"
    )
    @PostMapping("/sync")
    public ResponseEntity<BsaleSyncResultDto> sincronizarProductos() {
        return ResponseEntity.ok(bsaleProductSyncService.sincronizarProductos());
    }

    @Operation(summary = "Estado de la última sincronización Bsale")
    @GetMapping("/sync/ultima")
    public ResponseEntity<IntegracionSyncLog> ultimaSincronizacion() {
        IntegracionSyncLog log = bsaleProductSyncService.obtenerUltimaSincronizacion();
        if (log == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(log);
    }
}
