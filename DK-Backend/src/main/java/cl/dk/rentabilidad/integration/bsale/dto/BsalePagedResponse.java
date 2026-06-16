package cl.dk.rentabilidad.integration.bsale.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BsalePagedResponse<T> {

    private Integer count;
    private Integer limit;
    private Integer offset;
    private List<T> items;
    private String next;
}
