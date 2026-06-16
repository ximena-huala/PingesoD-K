package cl.dk.rentabilidad.integration.falabella.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Un producto del catálogo (GetProducts). Campos en PascalCase como los manda
 * Falabella. El PrimaryCategoryId sirve para cruzar con GetCategoryTree y sacar
 * la tarifa de comisión de esa categoría.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDto(
    String SellerSku,
    String ShopSku,
    String Name,
    String Brand,
    String PrimaryCategoryId,
    String Price,
    String Status
) {
}
