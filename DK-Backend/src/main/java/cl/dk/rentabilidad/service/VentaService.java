package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.Venta;
import cl.dk.rentabilidad.exception.ResourceNotFoundException;
import cl.dk.rentabilidad.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Gestiona el ciclo de vida de las ventas.
 *
 * <p>Cada alta o modificación dispara el recálculo de rentabilidad para mantener
 * los márgenes sincronizados con el catálogo y los costos del canal.
 */
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final RentabilidadService rentabilidadService;

    public Venta obtenerPorId(UUID id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));
    }

    /** Registra la venta y calcula su rentabilidad en la misma transacción. */
    @Transactional
    public Venta registrar(Venta venta) {
        validarVenta(venta);

        Venta ventaGuardada = ventaRepository.save(venta);
        rentabilidadService.calcular(ventaGuardada);
        return ventaGuardada;
    }

    /** Actualiza la venta y recalcula la rentabilidad asociada. */
    @Transactional
    public Venta actualizar(UUID id, Venta datos) {
        validarVenta(datos);

        Venta existente = obtenerPorId(id);
        existente.setCanal(datos.getCanal());
        existente.setProducto(datos.getProducto());
        existente.setFechaVenta(datos.getFechaVenta());
        existente.setPrecioVenta(datos.getPrecioVenta());
        existente.setCantidad(datos.getCantidad() != null ? datos.getCantidad() : 1);
        existente.setDescuentoCampana(
                datos.getDescuentoCampana() != null ? datos.getDescuentoCampana() : existente.getDescuentoCampana());
        existente.setReferenciaExterna(datos.getReferenciaExterna());

        Venta ventaGuardada = ventaRepository.save(existente);
        rentabilidadService.calcular(ventaGuardada);
        return ventaGuardada;
    }

    /** Elimina la venta y su registro de rentabilidad. */
    @Transactional
    public void eliminar(UUID id) {
        obtenerPorId(id);
        rentabilidadService.eliminarPorVenta(id);
        ventaRepository.deleteById(id);
    }

    /**
     * @param desde     inicio del rango (inclusive)
     * @param hasta     fin del rango (inclusive)
     * @param canalId   filtro opcional por canal
     * @param categoria filtro opcional por categoría de producto
     */
    public List<Venta> filtrar(LocalDate desde,
                               LocalDate hasta,
                               UUID canalId,
                               String categoria) {
        return ventaRepository.filtrar(desde, hasta, canalId, categoria);
    }

    public List<Venta> listarPorCanal(UUID canalId) {
        return ventaRepository.findByCanalId(canalId);
    }

    private void validarVenta(Venta venta) {
        if (venta.getCanal() == null || venta.getCanal().getId() == null) {
            throw new IllegalArgumentException("El canal es obligatorio");
        }
        if (venta.getProducto() == null || venta.getProducto().getId() == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (venta.getFechaVenta() == null) {
            throw new IllegalArgumentException("La fecha de venta es obligatoria");
        }
        if (venta.getPrecioVenta() == null) {
            throw new IllegalArgumentException("El precio de venta es obligatorio");
        }
    }
}
