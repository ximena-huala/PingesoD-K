package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.Producto;
import cl.dk.rentabilidad.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio que gestiona el catálogo de productos de D&K.
 *
 * El costo base de cada producto es fundamental para el cálculo
 * de rentabilidad, ya que representa lo que le costó a la empresa
 * adquirir o fabricar el producto.
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    /**
     * Retorna todos los productos activos del catálogo.
     */
    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrue();
    }

    /**
     * Busca un producto por su SKU único.
     * Se usa principalmente durante la importación de ventas
     * para asociar cada línea del archivo al producto correcto.
     *
     * @param sku código único del producto
     * @return producto encontrado
     * @throws RuntimeException si el SKU no existe en el catálogo
     */
    public Producto buscarPorSku(String sku) {
        return productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con SKU: " + sku));
    }

    /**
     * Crea o actualiza un producto en el catálogo.
     * Si el SKU ya existe, actualiza el registro existente.
     * Si es nuevo, lo crea.
     *
     * @param producto entidad con los datos del producto
     * @return producto persistido
     */
    @Transactional
    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    /**
     * Desactiva un producto del catálogo (borrado lógico).
     * No eliminamos físicamente para mantener el historial de ventas.
     *
     * @param id UUID del producto a desactivar
     */
    @Transactional
    public void desactivar(UUID id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));

        // Borrado lógico: marcamos como inactivo en vez de eliminar
        // para preservar la integridad del historial de ventas
        producto.setActivo(false);
        productoRepository.save(producto);
    }
}