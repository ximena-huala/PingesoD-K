package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.service.ReporteExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controlador que genera y descarga el reporte Excel.
 *
 * El reporte incluye 3 pestañas según lo acordado con el cliente:
 *   - Por producto
 *   - Por canal de venta
 *   - Por categoría
 *
 * Se genera en el backend con Apache POI y se envía como stream
 * para que el navegador lo descargue directamente.
 */
@RestController
@RequestMapping("/api/reporte")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteExcelService reporteExcelService;

    /**
     * Genera y descarga el reporte Excel con los filtros indicados.
     *
     * El archivo se genera al momento de la petición y se envía
     * como descarga directa con el nombre dk_reporte_{fecha}.xlsx
     *
     * @param desde     fecha inicio del rango (requerida)
     * @param hasta     fecha fin del rango (requerida)
     * @param canalId   UUID del canal a filtrar (opcional)
     * @param categoria categoría de producto a filtrar (opcional)
     * @return archivo Excel como stream descargable
     */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID canalId,
            @RequestParam(required = false) String categoria) throws Exception {

        // Generamos el archivo Excel en memoria
        byte[] archivo = reporteExcelService.generar(desde, hasta, canalId, categoria);

        // Nombre del archivo con fecha de generación para facilitar identificación
        String nombreArchivo = "dk_reporte_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                // Indicamos que es un archivo descargable
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\"")
                // Tipo MIME para archivos Excel
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }
}