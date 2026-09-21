package mexa.club.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtSessionProperties {

    /**
     * Access token TTL in seconds.
     */
    private long accessTokenTtl = 900;

    /**
     * Refresh token TTL in seconds.
     */
    private long refreshTokenTtl = 604_800;
}
