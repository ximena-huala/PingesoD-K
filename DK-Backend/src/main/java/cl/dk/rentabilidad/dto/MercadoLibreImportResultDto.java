package cl.dk.rentabilidad.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MercadoLibreImportResultDto {

    private final int creados;
    private final int actualizados;
    private final int omitidos;
    private final int errores;
    private final int totalProcesados;
    private final LocalDateTime importadoEn;
    private final List<String> detalleErrores;
}
