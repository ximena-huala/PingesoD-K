package cl.dk.rentabilidad.integration.falabella.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Una orden (GetOrders / GetOrder). Campos en PascalCase como vienen de Falabella.
 * Ojo: para rentabilidad lo que importa es el estado de cada item, no el de la
 * orden, así que el detalle fino sale de OrderItemDto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderDto(
    String OrderId,
    String OrderNumber,
    String CreatedAt,
    String UpdatedAt,
    String Price,
    String PaymentMethod,
    Statuses Statuses
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Statuses(List<String> Status) {
    }
}
