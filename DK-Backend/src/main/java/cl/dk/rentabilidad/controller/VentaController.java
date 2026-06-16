package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.entity.Venta;
import cl.dk.rentabilidad.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API REST de ventas.
 *
 * <p>Cada venta registrada dispara automáticamente el cálculo de rentabilidad
 * en {@link cl.dk.rentabilidad.service.RentabilidadService}.
 */
@Tag(name = "Ventas", description = "Registro, consulta y gestión de ventas por canal")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @Operation(summary = "Registrar venta", description = "Calcula rentabilidad automáticamente")
    @ApiResponse(responseCode = "201", description = "Venta registrada")
    @PostMapping
    public ResponseEntity<Venta> registrar(@RequestBody Venta venta) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventaService.registrar(venta));
    }

    @Operation(summary = "Obtener venta por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Venta> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ventaService.obtenerPorId(id));
    }

    @Operation(summary = "Actualizar venta", description = "Recalcula la rentabilidad asociada")
    @PutMapping("/{id}")
    public ResponseEntity<Venta> actualizar(
            @PathVariable UUID id,
            @RequestBody Venta venta) {
        return ResponseEntity.ok(ventaService.actualizar(id, venta));
    }

    @Operation(summary = "Eliminar venta", description = "Elimina también el registro de rentabilidad")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        ventaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Filtrar ventas por rango de fechas")
    @GetMapping
    public ResponseEntity<List<Venta>> filtrar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID canalId,
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(ventaService.filtrar(desde, hasta, canalId, categoria));
    }

    @Operation(summary = "Listar ventas de un canal")
    @GetMapping("/canal/{canalId}")
    public ResponseEntity<List<Venta>> porCanal(@PathVariable UUID canalId) {
        return ResponseEntity.ok(ventaService.listarPorCanal(canalId));
    }
}
