package mexa.club.shopservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.product-service")
public record ProductServiceProperties(
        @NotBlank String url,
        @Min(1) int timeoutMs,
        @Min(1) int defaultPageSize
) {
}
