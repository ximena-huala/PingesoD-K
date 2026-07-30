package cl.dk.rentabilidad.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Rentabilidad de una venta, ya calculada, para la tabla de detalle del frontend.
 * Combina datos de la venta (canal, producto, precio) con el resultado del motor
 * (ingreso neto, costos y margen).
 */
public record RentabilidadDetalleDto(
        UUID ventaId,
        LocalDate fecha,
        String canal,
        String sku,
        String producto,
        String categoria,
        BigDecimal precioVenta,
        BigDecimal descuento,
        BigDecimal ingresoNeto,
        BigDecimal costoProducto,
        BigDecimal comision,
        BigDecimal logistica,
        BigDecimal costoOperacional,
        BigDecimal costoTotal,
        BigDecimal margen,
        BigDecimal margenPorcentaje
) {}
