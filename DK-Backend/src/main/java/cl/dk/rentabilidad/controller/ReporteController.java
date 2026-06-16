package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.service.ReporteExcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Generación y descarga de reportes Excel.
 *
 * <p>El archivo incluye tres pestañas: por producto, por canal y por categoría.
 * Se genera en memoria con Apache POI y se entrega como descarga directa.
 */
@Tag(name = "Reportes", description = "Exportación de rentabilidad a Excel")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/reporte")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteExcelService reporteExcelService;

    @Operation(summary = "Descargar reporte Excel",
            description = "Genera un .xlsx con rentabilidad filtrada por fechas, canal y categoría")
    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID canalId,
            @RequestParam(required = false) String categoria) throws Exception {

        byte[] archivo = reporteExcelService.generar(desde, hasta, canalId, categoria);
        String nombreArchivo = "dk_reporte_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }
}
