package mexa.club.discountservice.security;

public final class InternalSecretValidator {

    private static final String INSECURE_DEFAULT = "change-me-internal";

    private InternalSecretValidator() {
    }

    public static void validate(String internalSecret) {
        if (internalSecret == null || internalSecret.isBlank() || INSECURE_DEFAULT.equals(internalSecret)) {
            throw new IllegalStateException(
                    "Invalid app.internal-secret value. Set a strong non-default secret before startup.");
        }
    }
}
