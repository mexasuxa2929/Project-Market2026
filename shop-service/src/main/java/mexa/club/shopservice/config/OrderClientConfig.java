package mexa.club.shopservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OrderServiceProperties.class)
public class OrderClientConfig {

    @Bean
    public RestClient orderRestClient(RestTemplateBuilder restTemplateBuilder, OrderServiceProperties properties) {
        long ms = properties.timeoutMs() > 0 ? properties.timeoutMs() : 8000L;
        Duration timeout = Duration.ofMillis(ms);
        return RestClient.builder(
                        restTemplateBuilder
                                .setConnectTimeout(timeout)
                                .setReadTimeout(timeout)
                                .build()
                )
                .baseUrl(properties.url())
                .build();
    }
}
