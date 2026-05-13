package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.entity.Venta;
import cl.dk.rentabilidad.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controlador para registro y consulta de ventas.
 *
 * Las ventas pueden venir de dos fuentes:
 * 1. Registro unitario manual via POST /api/ventas
 * 2. Importación masiva via POST /api/ventas/importar (próximo sprint)
 */
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    /**
     * Registra una venta individual y calcula su rentabilidad.
     *
     * @param venta datos de la venta a registrar
     * @return venta persistida con UUID asignado
     */
    @PostMapping
    public ResponseEntity<Venta> registrar(@RequestBody Venta venta) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventaService.registrar(venta));
    }

    /**
     * Filtra ventas por rango de fechas, canal y categoría.
     * Todos los parámetros excepto desde/hasta son opcionales.
     *
     * Ejemplo de uso:
     * GET /api/ventas?desde=2026-01-01&hasta=2026-03-31&canalId=uuid&categoria=Hogar
     *
     * @param desde     fecha inicio (requerida)
     * @param hasta     fecha fin (requerida)
     * @param canalId   UUID del canal (opcional)
     * @param categoria nombre de la categoría (opcional)
     * @return lista de ventas filtradas
     */
    @GetMapping
    public ResponseEntity<List<Venta>> filtrar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID canalId,
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(ventaService.filtrar(desde, hasta, canalId, categoria));
    }

    /**
     * Lista todas las ventas de un canal específico.
     *
     * @param canalId UUID del canal
     * @return lista de ventas del canal
     */
    @GetMapping("/canal/{canalId}")
    public ResponseEntity<List<Venta>> porCanal(@PathVariable UUID canalId) {
        return ResponseEntity.ok(ventaService.listarPorCanal(canalId));
    }
}
