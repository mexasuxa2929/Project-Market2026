package mexa.club.authservice.exception;

public class OtpTooManyAttemptsException extends RuntimeException {
    public OtpTooManyAttemptsException(String message) {
        super(message);
    }
}

