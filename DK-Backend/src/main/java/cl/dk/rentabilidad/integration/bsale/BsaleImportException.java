package cl.dk.rentabilidad.integration.bsale;

/**
 * Error al parsear o procesar archivos exportados desde Bsale.
 */
public class BsaleImportException extends RuntimeException {

    public BsaleImportException(String message) {
        super(message);
    }

    public BsaleImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
