package mexa.club.shopservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.order-service")
public record OrderServiceProperties(String url, long timeoutMs) {
}
