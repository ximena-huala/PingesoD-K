package cl.dk.rentabilidad.exception;

/**
 * Error de autenticación o credenciales inválidas.
 * Traducida a HTTP 401 por {@link GlobalExceptionHandler}.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
