package mexa.club.shopservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(ProductServiceProperties.class)
public class ProductClientConfig {

    @Bean
    public RestClient productRestClient(RestTemplateBuilder restTemplateBuilder, ProductServiceProperties properties) {
        Duration timeout = Duration.ofMillis(properties.timeoutMs());
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
