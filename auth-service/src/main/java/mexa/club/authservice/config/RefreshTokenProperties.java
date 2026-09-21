package mexa.club.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.refresh-token")
public class RefreshTokenProperties {

    private long ttlSeconds = 1_209_600;

    private Cookie cookie = new Cookie();

    private Cleanup cleanup = new Cleanup();

    @Getter
    @Setter
    public static class Cookie {
        private boolean enabled = true;
        private String name = "refreshToken";
        private String path = "/api/auth";
        private String sameSite = "Lax";
        private boolean secure = false;
    }

    @Getter
    @Setter
    public static class Cleanup {
        private boolean enabled = true;
        private long schedulerIntervalMs = 600_000;
    }
}
