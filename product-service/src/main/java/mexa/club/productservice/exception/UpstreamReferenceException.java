package mexa.club.productservice.exception;

/**
 * Reference (warehouse) katalogiga HTTP proxyni bajarishda xatolik.
 */
public class UpstreamReferenceException extends RuntimeException {

    public UpstreamReferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
