package cl.dk.rentabilidad.exception;

/**
 * Indica que un recurso solicitado no existe en la base de datos.
 * Traducida a HTTP 404 por {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
