package mexa.club.analyticsservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class InternalSecretFeignConfig {

    @Bean
    public RequestInterceptor analyticsInternalSecretInterceptor(@Value("${app.internal-secret}") String secret) {
        return template -> template.header("X-Internal-Secret", secret);
    }
}
