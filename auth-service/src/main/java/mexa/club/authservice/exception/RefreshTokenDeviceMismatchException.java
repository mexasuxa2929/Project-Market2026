package mexa.club.authservice.exception;

/**
 * Refresh token boshqa qurilmadan ishlatilayotganda tashlanadi (device binding buzilgan).
 * AuthExceptionHandler tomonidan 401 (UNAUTHORIZED) sifatida qaytariladi.
 */
public class RefreshTokenDeviceMismatchException extends RuntimeException {

    public RefreshTokenDeviceMismatchException() {
        super("Refresh token is bound to a different device");
    }
}
