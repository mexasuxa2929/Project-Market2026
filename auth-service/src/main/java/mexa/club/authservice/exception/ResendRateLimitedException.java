package mexa.club.authservice.exception;

public class ResendRateLimitedException extends RuntimeException {
    private final long waitSeconds;

    public ResendRateLimitedException(long waitSeconds, String message) {
        super(message);
        this.waitSeconds = waitSeconds;
    }

    public long getWaitSeconds() {
        return waitSeconds;
    }
}

