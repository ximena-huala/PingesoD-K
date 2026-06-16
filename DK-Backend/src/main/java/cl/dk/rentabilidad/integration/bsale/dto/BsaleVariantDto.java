package cl.dk.rentabilidad.integration.bsale.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BsaleVariantDto {

    private Integer id;
    /** SKU en Bsale. */
    private String code;
    private String description;
    /** 0 = activo, 1 = inactivo. */
    private Integer state;
    private BsaleRefDto product;
}
