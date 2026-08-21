package cl.dk.rentabilidad.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MercadoLibreCostoDto {

    private final UUID id;
    private final String sku;
    private final BigDecimal costoProm;
    private final BigDecimal ultimoCosto;
    private final BigDecimal costoMercadoLibre;
    private final String fuenteArchivo;
    private final LocalDateTime updatedAt;
}
