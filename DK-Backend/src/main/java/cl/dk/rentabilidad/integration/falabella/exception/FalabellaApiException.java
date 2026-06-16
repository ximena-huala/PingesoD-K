package cl.dk.rentabilidad.integration.falabella.exception;

/**
 * Lo que lanzamos cuando Falabella responde con error, o cuando no pudimos
 * parsear la respuesta. El errorCode ayuda a saber qué pasó:
 *
 *   E001 - falta un parámetro obligatorio (típico: Timestamp o el User-Agent)
 *   E007 - la firma no calza (algo en el HMAC quedó mal)
 *   E008 - Action inválido, o el endpoint no está habilitado para la cuenta
 *   E016 - OrderId que no existe
 *   105  - sin permisos para ese endpoint
 */
public class FalabellaApiException extends RuntimeException {

    private final String errorCode;
    private final String action;

    public FalabellaApiException(String errorCode, String message, String action) {
        super("[" + action + "] " + errorCode + ": " + message);
        this.errorCode = errorCode;
        this.action = action;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getAction() {
        return action;
    }
}
