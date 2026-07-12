package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.entity.IntegracionSyncLog;
import cl.dk.rentabilidad.integration.bsale.BsaleManualImportService;
import cl.dk.rentabilidad.integration.bsale.BsaleProductSyncService;
import cl.dk.rentabilidad.integration.bsale.dto.BsaleSyncResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Integración con Bsale (catálogo maestro).
 *
 * <p>Carga manual (recomendada): exportar archivos desde el panel Bsale y subirlos aquí.
 * La sincronización por API queda disponible cuando se obtenga el token.
 */
@Tag(name = "Integración Bsale", description = "Importación manual y sincronización API del catálogo Bsale")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/integraciones/bsale")
@RequiredArgsConstructor
public class BsaleController {

    private final BsaleManualImportService bsaleManualImportService;
    private final BsaleProductSyncService bsaleProductSyncService;

    @Operation(
            summary = "Importar catálogo desde archivos Bsale",
            description = """
                    Sube exportaciones descargadas desde Bsale (Excel o CSV).
                    - **productos**: módulo Productos y servicios → SKU, estado, marca, tipo
                    - **stock**: módulo Stock actual → stock y costo promedio
                    Puedes enviar uno o ambos archivos en la misma petición.
                    """
    )
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BsaleSyncResultDto> importarManual(
            @RequestPart(value = "productos", required = false) MultipartFile archivoProductos,
            @RequestPart(value = "stock", required = false) MultipartFile archivoStock) {
        return ResponseEntity.ok(bsaleManualImportService.importar(archivoProductos, archivoStock));
    }

    @Operation(summary = "Estado de la última importación manual")
    @GetMapping("/import/ultima")
    public ResponseEntity<IntegracionSyncLog> ultimaImportacionManual() {
        IntegracionSyncLog log = bsaleManualImportService.obtenerUltimaImportacion();
        if (log == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(log);
    }

    @Operation(
            summary = "Sincronizar productos vía API Bsale",
            description = "Requiere BSALE_ACCESS_TOKEN. Usar /import si no hay token."
    )
    @PostMapping("/sync")
    public ResponseEntity<BsaleSyncResultDto> sincronizarProductos() {
        return ResponseEntity.ok(bsaleProductSyncService.sincronizarProductos());
    }

    @Operation(summary = "Estado de la última sincronización API")
    @GetMapping("/sync/ultima")
    public ResponseEntity<IntegracionSyncLog> ultimaSincronizacion() {
        IntegracionSyncLog log = bsaleProductSyncService.obtenerUltimaSincronizacion();
        if (log == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(log);
    }
}
