package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.Producto;
import cl.dk.rentabilidad.exception.ConflictException;
import cl.dk.rentabilidad.exception.ResourceNotFoundException;
import cl.dk.rentabilidad.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestiona el catálogo maestro de productos de D&K.
 *
 * <p>El {@link Producto#getCostoBase()} es la base del cálculo de rentabilidad.
 * El SKU actúa como identificador único transversal a todos los marketplaces.
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    /** @return productos con {@code activo = true} */
    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrue();
    }

    /** @return todos los productos, incluidos los desactivados */
    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    /**
     * @param id UUID del producto
     * @return producto encontrado
     * @throws ResourceNotFoundException si no existe
     */
    public Producto obtenerPorId(UUID id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    /**
     * @param sku código único del producto
     * @return producto asociado al SKU
     * @throws ResourceNotFoundException si el SKU no está registrado
     */
    public Producto buscarPorSku(String sku) {
        return productoRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con SKU: " + sku));
    }

    /**
     * Crea un producto nuevo validando unicidad del SKU.
     *
     * @throws ConflictException si el SKU ya existe
     * @throws IllegalArgumentException si faltan campos obligatorios
     */
    @Transactional
    public Producto crear(Producto producto) {
        validarProducto(producto);

        productoRepository.findBySku(producto.getSku().trim()).ifPresent(p -> {
            throw new ConflictException("Ya existe un producto con SKU: " + producto.getSku());
        });

        producto.setSku(producto.getSku().trim());
        producto.setActivo(producto.getActivo() != null ? producto.getActivo() : true);
        return productoRepository.save(producto);
    }

    /**
     * Actualiza un producto existente. Si cambia el SKU, valida que el nuevo no esté en uso.
     */
    @Transactional
    public Producto actualizar(UUID id, Producto datos) {
        validarProducto(datos);

        Producto existente = obtenerPorId(id);
        String nuevoSku = datos.getSku().trim();

        if (!existente.getSku().equals(nuevoSku)) {
            productoRepository.findBySku(nuevoSku).ifPresent(p -> {
                throw new ConflictException("Ya existe un producto con SKU: " + nuevoSku);
            });
        }

        existente.setSku(nuevoSku);
        existente.setNombre(datos.getNombre().trim());
        existente.setCategoria(datos.getCategoria());
        existente.setMarca(datos.getMarca());
        existente.setTipoProducto(datos.getTipoProducto());
        if (datos.getStock() != null) {
            existente.setStock(datos.getStock());
        }
        existente.setCostoBase(datos.getCostoBase());
        if (datos.getActivo() != null) {
            existente.setActivo(datos.getActivo());
        }

        return productoRepository.save(existente);
    }

    /**
     * Desactiva el producto (borrado lógico) para preservar el historial de ventas.
     */
    @Transactional
    public void desactivar(UUID id) {
        Producto producto = obtenerPorId(id);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    /**
     * Crea o actualiza un producto a partir de datos sincronizados desde Bsale.
     * Busca primero por {@code bsaleVariantId}, luego por SKU.
     *
     * @return {@code true} si se creó, {@code false} si se actualizó
     */
    @Transactional
    public boolean sincronizarDesdeBsale(Integer bsaleVariantId,
                                         Integer bsaleProductId,
                                         String sku,
                                         String nombre,
                                         String categoria,
                                         BigDecimal costoBase,
                                         boolean activo) {
        return importarDesdeBsale(sku, nombre, null, null, categoria, activo, null, costoBase,
                bsaleVariantId, bsaleProductId);
    }

    /**
     * Crea o actualiza un producto desde exportación manual de Bsale (sin API).
     * Los campos {@code null} no sobrescriben valores existentes.
     *
     * @return {@code true} si se creó, {@code false} si se actualizó
     */
    @Transactional
    public boolean importarDesdeBsale(String sku,
                                        String nombre,
                                        String marca,
                                        String tipoProducto,
                                        String categoria,
                                        Boolean activo,
                                        BigDecimal stock,
                                        BigDecimal costoBase) {
        return importarDesdeBsale(sku, nombre, marca, tipoProducto, categoria, activo, stock, costoBase,
                null, null);
    }

    public boolean existePorSku(String sku) {
        return productoRepository.findBySku(sku.trim()).isPresent();
    }

    private boolean importarDesdeBsale(String sku,
                                       String nombre,
                                       String marca,
                                       String tipoProducto,
                                       String categoria,
                                       Boolean activo,
                                       BigDecimal stock,
                                       BigDecimal costoBase,
                                       Integer bsaleVariantId,
                                       Integer bsaleProductId) {
        String skuNormalizado = sku.trim();
        if (skuNormalizado.isBlank()) {
            throw new IllegalArgumentException("El SKU es obligatorio");
        }

        Producto producto = productoRepository.findBySku(skuNormalizado)
                .or(() -> bsaleVariantId != null
                        ? productoRepository.findByBsaleVariantId(bsaleVariantId)
                        : Optional.empty())
                .orElse(null);

        boolean esNuevo = producto == null;

        if (esNuevo) {
            producto = Producto.builder()
                    .sku(skuNormalizado)
                    .nombre(nombre != null && !nombre.isBlank() ? nombre.trim() : skuNormalizado)
                    .marca(marca)
                    .tipoProducto(tipoProducto)
                    .categoria(categoria)
                    .costoBase(costoBase != null ? costoBase : BigDecimal.ZERO)
                    .stock(stock != null ? stock : BigDecimal.ZERO)
                    .bsaleVariantId(bsaleVariantId)
                    .bsaleProductId(bsaleProductId)
                    .activo(activo != null ? activo : true)
                    .build();
        } else {
            producto.setSku(skuNormalizado);
            if (nombre != null && !nombre.isBlank()) {
                producto.setNombre(nombre.trim());
            }
            if (marca != null && !marca.isBlank()) {
                producto.setMarca(marca.trim());
            }
            if (tipoProducto != null && !tipoProducto.isBlank()) {
                producto.setTipoProducto(tipoProducto.trim());
            }
            if (categoria != null && !categoria.isBlank()) {
                producto.setCategoria(categoria.trim());
            }
            if (activo != null) {
                producto.setActivo(activo);
            }
            if (stock != null) {
                producto.setStock(stock);
            }
            // No pisar costos buenos con 0 (variantes Bsale sin stock)
            if (costoBase != null && costoBase.signum() > 0) {
                producto.setCostoBase(costoBase);
            }
            if (bsaleVariantId != null) {
                producto.setBsaleVariantId(bsaleVariantId);
            }
            if (bsaleProductId != null) {
                producto.setBsaleProductId(bsaleProductId);
            }
        }

        productoRepository.save(producto);
        return esNuevo;
    }

    private void validarProducto(Producto producto) {
        if (producto.getSku() == null || producto.getSku().isBlank()) {
            throw new IllegalArgumentException("El SKU es obligatorio");
        }
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (producto.getCostoBase() == null) {
            throw new IllegalArgumentException("El costo base es obligatorio");
        }
    }
}
