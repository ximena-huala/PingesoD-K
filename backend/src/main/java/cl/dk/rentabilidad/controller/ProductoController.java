package cl.dk.rentabilidad.controller;

import cl.dk.rentabilidad.entity.Producto;
import cl.dk.rentabilidad.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador para gestión del catálogo de productos de D&K.
 *
 * El costo base de cada producto es fundamental para el cálculo
 * de rentabilidad, por lo que mantener este catálogo actualizado
 * es responsabilidad del equipo administrador.
 */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Lista todos los productos activos del catálogo.
     */
    @GetMapping
    public ResponseEntity<List<Producto>> listarActivos() {
        return ResponseEntity.ok(productoService.listarActivos());
    }

    /**
     * Crea o actualiza un producto en el catálogo.
     *
     * @param producto datos del producto
     * @return producto persistido con su UUID
     */
    @PostMapping
    public ResponseEntity<Producto> guardar(@RequestBody Producto producto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.guardar(producto));
    }

    /**
     * Busca un producto por su SKU.
     * Útil para verificar si un producto ya existe antes de importar ventas.
     *
     * @param sku código único del producto
     * @return producto encontrado
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<Producto> buscarPorSku(@PathVariable String sku) {
        return ResponseEntity.ok(productoService.buscarPorSku(sku));
    }

    /**
     * Desactiva un producto del catálogo (borrado lógico).
     * No se elimina físicamente para preservar el historial de ventas.
     *
     * @param id UUID del producto a desactivar
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        productoService.desactivar(id);
        // 204 No Content: operación exitosa sin cuerpo de respuesta
        return ResponseEntity.noContent().build();
    }
}