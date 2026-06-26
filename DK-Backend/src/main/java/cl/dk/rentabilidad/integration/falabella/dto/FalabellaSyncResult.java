package cl.dk.rentabilidad.integration.falabella.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumen de una corrida de sincronización de ventas desde Falabella.
 * Lo importante para el equipo es {@code skusSinProducto}: los SKU de ventas
 * que no encontraron su producto en la base (normalmente porque no están en
 * Bsale), que es la lista de excepciones a revisar con el cliente.
 */
public record FalabellaSyncResult(
    int ordenesProcesadas,
    int itemsTotal,
    int itemsNoEntregados,
    int ventasCreadas,
    int ventasActualizadas,
    int itemsSinProducto,
    List<String> skusSinProducto,
    LocalDateTime sincronizadoEn
) {
}
