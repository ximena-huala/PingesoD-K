package cl.dk.rentabilidad.integration.falabella.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Un item de una orden, con los montos que necesita el cálculo de rentabilidad
 * (GetOrderItems). Los campos van en PascalCase porque los dejamos igual que
 * como llegan de Falabella.
 *
 * Para tener a mano cómo se usan estos montos (lo verificamos con la orden real
 * 1139186719):
 *   ingreso bruto = PaidPrice + ShippingAmount
 *   margen        = ingreso bruto - costo del producto (eso lo pone Bsale)
 *                   - comisión (PaidPrice × tarifa de la categoría)
 *                   - ShippingServiceCost - VoucherAmount
 *
 * Importante: solo cuentan los items "delivered", y ese estado va por item, no
 * por la orden completa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderItemDto(
    String OrderItemId,
    String OrderId,
    String Sku,
    String ShopSku,
    String Name,
    String PaidPrice,
    String ItemPrice,
    String ShippingAmount,
    String ShippingServiceCost,
    String VoucherAmount,
    String Status
) {
    /** Solo los entregados entran al cálculo de rentabilidad. */
    public boolean isDelivered() {
        return "delivered".equalsIgnoreCase(Status);
    }
}
