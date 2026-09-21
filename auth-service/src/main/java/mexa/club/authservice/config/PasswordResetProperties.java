package mexa.club.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

    private long ttlMinutes = 15;

    private String resetUrlBase = "https://domain.com/reset-password";

    private Cleanup cleanup = new Cleanup();

    @Getter
    @Setter
    public static class Cleanup {
        private boolean enabled = true;
        private long schedulerIntervalMs = 600_000;
    }
}
