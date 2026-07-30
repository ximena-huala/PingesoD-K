package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.dto.RentabilidadDetalleDto;
import cl.dk.rentabilidad.dto.RentabilidadResumenDto;
import cl.dk.rentabilidad.service.RentabilidadConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Rentabilidad en JSON para el frontend.
 *
 * Misma información que el reporte Excel, pero consumible por la app:
 *   - /resumen: totales + desglose por categoría, canal, mes y producto.
 *   - /detalle: una fila por venta, con ingreso, costos y margen.
 *
 * Las fechas son opcionales; si no se pasan, toma todo el histórico.
 */
@Tag(name = "Rentabilidad", description = "Rentabilidad agregada y por venta, en JSON")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/rentabilidad")
@RequiredArgsConstructor
public class RentabilidadController {

    private final RentabilidadConsultaService service;

    @Operation(summary = "Resumen de rentabilidad",
            description = "Totales del período y desglose por categoría, canal, mes y producto")
    @GetMapping("/resumen")
    public ResponseEntity<RentabilidadResumenDto> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID canalId,
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(
                service.resumen(desdeODefault(desde), hastaODefault(hasta), canalId, categoria));
    }

    @Operation(summary = "Detalle de rentabilidad por venta",
            description = "Una fila por venta con precio, ingreso neto, costos y margen")
    @GetMapping("/detalle")
    public ResponseEntity<List<RentabilidadDetalleDto>> detalle(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID canalId,
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(
                service.detalle(desdeODefault(desde), hastaODefault(hasta), canalId, categoria));
    }

    private static LocalDate desdeODefault(LocalDate desde) {
        return desde != null ? desde : LocalDate.of(2000, 1, 1);
    }

    private static LocalDate hastaODefault(LocalDate hasta) {
        return hasta != null ? hasta : LocalDate.now();
    }
}
