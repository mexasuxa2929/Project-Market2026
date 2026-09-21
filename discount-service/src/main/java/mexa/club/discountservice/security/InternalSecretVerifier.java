package mexa.club.discountservice.security;

import mexa.club.discountservice.exception.DiscountException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InternalSecretVerifier {

    private final String expected;

    public InternalSecretVerifier(@Value("${app.internal-secret}") String expected) {
        this.expected = expected;
    }

    public void verify(String headerValue) {
        if (!StringUtils.hasText(headerValue) || !expected.equals(headerValue)) {
            throw new DiscountException(HttpStatus.FORBIDDEN, DiscountException.INTERNAL_SECRET_INVALID,
                    "Invalid or missing X-Internal-Secret");
        }
    }
}
