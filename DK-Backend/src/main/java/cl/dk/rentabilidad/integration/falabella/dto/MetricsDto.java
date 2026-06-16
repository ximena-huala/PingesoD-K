package cl.dk.rentabilidad.integration.falabella.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Métricas históricas del seller (GetMetrics): ventas totales, comisiones, etc.
 * Campos en PascalCase tal como los manda Falabella. Con esto se saca la
 * comisión histórica promedio (comisiones sobre ventas).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetricsDto(
    String StatisticsType,
    String SkuNumber,
    String SkuActive,
    String SalesTotal,
    String Orders,
    String Commissions,
    String ReturnsPercentage
) {
    /** Comisión histórica como porcentaje: Commissions / SalesTotal × 100 */
    public double commissionRate() {
        double sales = Double.parseDouble(SalesTotal);
        double comm = Double.parseDouble(Commissions);
        return sales == 0 ? 0 : (comm / sales) * 100;
    }
}
