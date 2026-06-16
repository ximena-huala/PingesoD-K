package cl.dk.rentabilidad.integration.bsale.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BsaleRefDto {

    private String href;
    private String id;
}
