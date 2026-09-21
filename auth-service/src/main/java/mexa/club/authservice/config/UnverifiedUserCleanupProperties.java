package mexa.club.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cleanup")
public class UnverifiedUserCleanupProperties {

    /**
     * Tasdiqlanmagan akkauntlarni shu vaqtdan eski bo‘lsa o‘chirish (daqiqa).
     */
    private int unverifiedUserMaxAgeMinutes = 60;
}
