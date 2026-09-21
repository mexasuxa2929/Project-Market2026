package mexa.club.geoservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalSecretRequestInterceptor(
            @Value("${app.internal.secret}") String internalSecret
    ) {
        return requestTemplate -> requestTemplate.header("X-Internal-Secret", internalSecret);
    }
}
