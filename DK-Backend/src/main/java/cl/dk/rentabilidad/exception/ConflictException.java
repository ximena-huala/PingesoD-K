package cl.dk.rentabilidad.exception;

/**
 * Indica un conflicto de unicidad o regla de negocio (ej. SKU duplicado).
 * Traducida a HTTP 409 por {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
