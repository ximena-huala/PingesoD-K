package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.entity.CanalVenta;
import cl.dk.rentabilidad.entity.CostoCanal;
import cl.dk.rentabilidad.service.CanalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API REST de canales de venta y sus costos operacionales.
 *
 * <p>Los costos (comisiones, envíos, logística) se configuran por canal y pueden
 * tener vigencia temporal mediante {@code fechaInicio} y {@code fechaFin}.
 */
@Tag(name = "Canales", description = "CRUD de canales de venta y costos operacionales")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/canales")
@RequiredArgsConstructor
public class CanalController {

    private final CanalService canalService;

    @Operation(summary = "Listar todos los canales")
    @GetMapping
    public ResponseEntity<List<CanalVenta>> listarTodos() {
        return ResponseEntity.ok(canalService.listarTodos());
    }

    @Operation(summary = "Listar canales activos")
    @GetMapping("/activos")
    public ResponseEntity<List<CanalVenta>> listarActivos() {
        return ResponseEntity.ok(canalService.listarActivos());
    }

    @Operation(summary = "Obtener canal por ID")
    @GetMapping("/{id}")
    public ResponseEntity<CanalVenta> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(canalService.obtenerPorId(id));
    }

    @Operation(summary = "Crear canal de venta")
    @ApiResponse(responseCode = "201", description = "Canal creado")
    @ApiResponse(responseCode = "409", description = "Nombre de canal duplicado")
    @PostMapping
    public ResponseEntity<CanalVenta> crear(@RequestBody CanalVenta canal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(canalService.crear(canal));
    }

    @Operation(summary = "Actualizar canal de venta")
    @PutMapping("/{id}")
    public ResponseEntity<CanalVenta> actualizar(
            @PathVariable UUID id,
            @RequestBody CanalVenta canal) {
        return ResponseEntity.ok(canalService.actualizar(id, canal));
    }

    @Operation(summary = "Desactivar canal", description = "Borrado lógico")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        canalService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar costos de un canal")
    @GetMapping("/{canalId}/costos")
    public ResponseEntity<List<CostoCanal>> listarCostos(@PathVariable UUID canalId) {
        return ResponseEntity.ok(canalService.listarCostos(canalId));
    }

    @Operation(summary = "Obtener un costo del canal")
    @GetMapping("/{canalId}/costos/{costoId}")
    public ResponseEntity<CostoCanal> obtenerCosto(
            @PathVariable UUID canalId,
            @PathVariable UUID costoId) {
        return ResponseEntity.ok(canalService.obtenerCosto(canalId, costoId));
    }

    @Operation(summary = "Agregar costo a un canal")
    @PostMapping("/{canalId}/costos")
    public ResponseEntity<CostoCanal> agregarCosto(
            @PathVariable UUID canalId,
            @RequestBody CostoCanal costo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(canalService.agregarCosto(canalId, costo));
    }

    @Operation(summary = "Actualizar costo de un canal")
    @PutMapping("/{canalId}/costos/{costoId}")
    public ResponseEntity<CostoCanal> actualizarCosto(
            @PathVariable UUID canalId,
            @PathVariable UUID costoId,
            @RequestBody CostoCanal costo) {
        return ResponseEntity.ok(canalService.actualizarCosto(canalId, costoId, costo));
    }

    @Operation(summary = "Eliminar costo de un canal")
    @DeleteMapping("/{canalId}/costos/{costoId}")
    public ResponseEntity<Void> eliminarCosto(
            @PathVariable UUID canalId,
            @PathVariable UUID costoId) {
        canalService.eliminarCosto(canalId, costoId);
        return ResponseEntity.noContent().build();
    }
}
