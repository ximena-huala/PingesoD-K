package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.entity.CanalVenta;
import cl.dk.rentabilidad.entity.CostoCanal;
import cl.dk.rentabilidad.service.CanalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador que expone los endpoints para gestión de canales de venta
 * y sus costos operacionales.
 *
 * Los costos se editan manualmente por el equipo de D&K,
 * por lo que este controlador es parte central del módulo de configuración.
 */
@RestController
@RequestMapping("/api/canales")
@RequiredArgsConstructor
public class CanalController {

    private final CanalService canalService;

    /**
     * Lista todos los canales de venta registrados.
     * Incluye activos e inactivos para la pantalla de configuración.
     */
    @GetMapping
    public ResponseEntity<List<CanalVenta>> listarTodos() {
        return ResponseEntity.ok(canalService.listarTodos());
    }

    /**
     * Lista solo los canales activos.
     * Se usa en los filtros del dashboard y del reporte.
     */
    @GetMapping("/activos")
    public ResponseEntity<List<CanalVenta>> listarActivos() {
        return ResponseEntity.ok(canalService.listarActivos());
    }

    /**
     * Crea un nuevo canal de venta.
     *
     * @param canal datos del canal a crear
     * @return canal creado con su UUID asignado
     */
    @PostMapping
    public ResponseEntity<CanalVenta> crear(@RequestBody CanalVenta canal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(canalService.crear(canal));
    }

    /**
     * Agrega un costo operacional a un canal existente.
     * Ejemplo: agregar comisión del 13% a MercadoLibre.
     *
     * @param canalId UUID del canal al que se agrega el costo
     * @param costo   datos del costo a agregar
     * @return costo creado con su UUID asignado
     */
    @PostMapping("/{canalId}/costos")
    public ResponseEntity<CostoCanal> agregarCosto(
            @PathVariable UUID canalId,
            @RequestBody CostoCanal costo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(canalService.agregarCosto(canalId, costo));
    }

    /**
     * Lista todos los costos configurados para un canal.
     * Incluye el historial completo, no solo los vigentes.
     *
     * @param canalId UUID del canal
     * @return lista de costos del canal
     */
    @GetMapping("/{canalId}/costos")
    public ResponseEntity<List<CostoCanal>> listarCostos(@PathVariable UUID canalId) {
        return ResponseEntity.ok(canalService.listarCostos(canalId));
    }
}