package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.dto.MercadoLibreCostoDto;
import cl.dk.rentabilidad.dto.MercadoLibreImportResultDto;
import cl.dk.rentabilidad.service.MercadoLibreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Integración MercadoLibre", description = "Importación y consulta de costos por SKU para MercadoLibre")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/integraciones/mercadolibre")
@RequiredArgsConstructor
public class MercadoLibreController {

    private final MercadoLibreService mercadoLibreService;

    @Operation(summary = "Importar costos de MercadoLibre desde CSV")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MercadoLibreImportResultDto> importar(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(mercadoLibreService.importarDesdeCsv(file));
    }

    @Operation(summary = "Listar costos importados de MercadoLibre")
    @GetMapping
    public ResponseEntity<List<MercadoLibreCostoDto>> listar() {
        return ResponseEntity.ok(mercadoLibreService.listar());
    }

    @Operation(summary = "Obtener costo por SKU")
    @GetMapping("/{sku}")
    public ResponseEntity<MercadoLibreCostoDto> obtenerPorSku(@PathVariable String sku) {
        return ResponseEntity.ok(mercadoLibreService.obtenerPorSku(sku));
    }

    @Operation(summary = "Exportar costos de MercadoLibre a CSV")
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportarCsv() {
        String csv = mercadoLibreService.exportarCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=costos-mercadolibre-export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
