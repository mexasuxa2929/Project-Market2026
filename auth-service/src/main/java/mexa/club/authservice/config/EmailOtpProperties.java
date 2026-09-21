package mexa.club.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.email-otp")
public class EmailOtpProperties {

    /**
     * OTP TTL sekundlarda (3-5 daqiqa).
     */
    private long ttlSeconds = 300;

    /**
     * Resend orasidagi minimal interval (kamida 60 soniya).
     */
    private long resendMinIntervalSeconds = 60;

    /**
     * Maksimal urinishlar.
     */
    private int maxAttempts = 5;

    /**
     * OTP hash uchun secret (sha256).
     */
    private String hashSecret = "change-me";
}

