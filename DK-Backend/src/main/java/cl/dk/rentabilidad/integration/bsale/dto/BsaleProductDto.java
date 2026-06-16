package cl.dk.rentabilidad.integration.bsale.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BsaleProductDto {

    private Integer id;
    private String name;
    private BsaleRefDto product_type;
}
