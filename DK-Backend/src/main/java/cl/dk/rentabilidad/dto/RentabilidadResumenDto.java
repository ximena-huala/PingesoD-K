package cl.dk.rentabilidad.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rentabilidad agregada para el dashboard del frontend.
 *
 * Trae los totales del período y el mismo desglose que el reporte Excel
 * (por categoría, canal, mes y producto), pero en JSON para pintarlo en
 * tarjetas, tablas y gráficos. El margen de cada grupo es ponderado
 * (margen total / ingreso total), que es el margen real del grupo.
 */
public record RentabilidadResumenDto(
        Totales totales,
        List<Grupo> porCategoria,
        List<Grupo> porCanal,
        List<GrupoMes> porMes,
        List<GrupoProducto> porProducto
) {
    public record Totales(
            long ventas,
            BigDecimal ingreso,
            BigDecimal costoProducto,
            BigDecimal costoOperacional,
            BigDecimal costoTotal,
            BigDecimal margen,
            BigDecimal margenPorcentaje
    ) {}

    public record Grupo(
            String etiqueta,
            long unidades,
            BigDecimal ingreso,
            BigDecimal costoTotal,
            BigDecimal margen,
            BigDecimal margenPorcentaje
    ) {}

    public record GrupoMes(
            String mes,
            long unidades,
            BigDecimal ingreso,
            BigDecimal margen,
            BigDecimal margenPorcentaje
    ) {}

    public record GrupoProducto(
            String sku,
            String nombre,
            String categoria,
            long unidades,
            BigDecimal ingreso,
            BigDecimal costoTotal,
            BigDecimal margen,
            BigDecimal margenPorcentaje
    ) {}
}
