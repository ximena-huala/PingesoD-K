package cl.dk.rentabilidad.dto;

import java.time.LocalDate;

/**
 * Estado de la integración con Falabella, para la pantalla de Integraciones del frontend.
 * Dice si hay credenciales, si la conexión responde en vivo, y cuántas ventas hay cargadas.
 */
public record FalabellaEstadoDto(
        String canal,
        String sellerId,
        boolean credencialesConfiguradas,
        boolean conexionOk,
        String mensaje,
        long ventasCargadas,
        LocalDate ultimaVenta
) {}
