package mexa.club.analyticsservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class WarehouseFeignConfig {

    @Bean
    public RequestInterceptor warehouseInternalApiKeyInterceptor(
            @Value("${app.analytics.warehouse-internal-api-key}") String apiKey
    ) {
        return template -> template.header("X-Internal-Api-Key", apiKey);
    }
}
