package cl.dk.rentabilidad.dto;

import cl.dk.rentabilidad.entity.Venta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista de una venta para la API, con el canal y el producto ya resueltos.
 *
 * Existe para no serializar la entidad directamente: la {@link Venta} tiene el
 * canal y el producto como lazy, y al convertirla a JSON fuera de una transacción
 * (open-in-view está apagado) reventaba. Mapeando a este record dentro de la
 * transacción, el JSON sale plano y sin sorpresas.
 */
public record VentaDto(
    UUID id,
    UUID canalId,
    String canal,
    UUID productoId,
    String sku,
    String producto,
    String categoria,
    LocalDate fechaVenta,
    BigDecimal precioVenta,
    Integer cantidad,
    BigDecimal descuentoCampana,
    String referenciaExterna,
    String numeroOrden,
    LocalDateTime createdAt
) {
    public static VentaDto de(Venta v) {
        var canal = v.getCanal();
        var prod = v.getProducto();
        return new VentaDto(
            v.getId(),
            canal != null ? canal.getId() : null,
            canal != null ? canal.getNombre() : null,
            prod != null ? prod.getId() : null,
            prod != null ? prod.getSku() : null,
            prod != null ? prod.getNombre() : null,
            prod != null ? prod.getCategoria() : null,
            v.getFechaVenta(),
            v.getPrecioVenta(),
            v.getCantidad(),
            v.getDescuentoCampana(),
            v.getReferenciaExterna(),
            v.getNumeroOrden(),
            v.getCreatedAt()
        );
    }
}
