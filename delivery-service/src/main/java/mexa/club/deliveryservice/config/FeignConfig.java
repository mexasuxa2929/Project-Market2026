package mexa.club.deliveryservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * order-service / internal/orders va notification-service /internal/notifications
 * uchun {@code X-Internal-Secret} (order va notification bilan bir xil kalit).
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalSecretRequestInterceptor(
            @Value("${app.internal.secret:${app.internal-secret}}") String internalSecret
    ) {
        return requestTemplate -> requestTemplate.header("X-Internal-Secret", internalSecret);
    }
}
