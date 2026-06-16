package cl.dk.rentabilidad.integration.bsale.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de una sincronización de catálogo desde Bsale.
 */
@Getter
@Builder
public class BsaleSyncResultDto {

    private final int productosCreados;
    private final int productosActualizados;
    private final int productosOmitidos;
    private final int errores;
    private final int totalProcesados;
    private final LocalDateTime sincronizadoEn;
    private final List<String> detalleErrores;
}
