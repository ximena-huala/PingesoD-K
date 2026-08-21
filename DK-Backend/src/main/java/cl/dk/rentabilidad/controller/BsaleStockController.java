package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.entity.Producto;
import cl.dk.rentabilidad.integration.bsale.BsaleManualImportService;
import cl.dk.rentabilidad.integration.bsale.dto.BsaleSyncResultDto;
import cl.dk.rentabilidad.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Carga de Stock actual de Bsale y consulta del catálogo de productos.
 *
 * <p>Requiere JWT y está disponible en todos los perfiles (dev y prod). Reemplaza
 * al antiguo {@code /api/dev/bsale}, que solo existía en perfil dev y sin autenticación.
 */
@Tag(name = "Bsale", description = "Importación de Stock actual y catálogo de productos")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/bsale")
public class BsaleStockController {

    private final BsaleManualImportService bsaleManualImportService;
    private final ProductoService productoService;

    public BsaleStockController(BsaleManualImportService bsaleManualImportService,
                                ProductoService productoService) {
        this.bsaleManualImportService = bsaleManualImportService;
        this.productoService = productoService;
    }

    @Operation(summary = "Importar Stock actual Bsale (XLSX/CSV)")
    @PostMapping(value = "/import/stock", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BsaleSyncResultDto> importarStock(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(bsaleManualImportService.importar(null, file));
    }

    @Operation(summary = "Listar productos del catálogo (post-importación)")
    @GetMapping("/productos")
    public ResponseEntity<List<Producto>> listarProductos(
            @RequestParam(defaultValue = "200") int limit) {
        List<Producto> todos = productoService.listarTodos();
        int max = Math.min(Math.max(limit, 1), 2000);
        if (todos.size() <= max) {
            return ResponseEntity.ok(todos);
        }
        return ResponseEntity.ok(todos.subList(0, max));
    }
}
