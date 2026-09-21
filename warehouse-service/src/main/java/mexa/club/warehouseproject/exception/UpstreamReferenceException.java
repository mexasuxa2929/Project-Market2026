package mexa.club.warehouseproject.exception;

/**
 * Tashqi reference/product service bilan aloqa xatolari.
 */
public class UpstreamReferenceException extends RuntimeException {

    public UpstreamReferenceException(String message) {
        super(message);
    }

    public UpstreamReferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
