package mexa.club.gatewayservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${gateway.cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${gateway.cors.allowed-origin-1:}")
    private String allowedOrigin1;

    @Value("${gateway.cors.allowed-origin-2:}")
    private String allowedOrigin2;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = collectOrigins();
        if (origins.isEmpty()) {
            // O-17: fail-open wildcard o'rniga — startni to'xtatish.
            // GATEWAY_CORS_ALLOWED_ORIGINS (yoki _1/_2) majburiy.
            throw new IllegalStateException(
                "O-17: gateway.cors.allowed-origins sozlanmagan. " +
                    "GATEWAY_CORS_ALLOWED_ORIGINS orqali ro'yxat bering (wildcard CORS taqiqlan).");
        } else {
            origins.forEach(config::addAllowedOrigin);
            log.info("CORS: allowed origins = {}", origins);
        }
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    private List<String> collectOrigins() {
        List<String> origins = new ArrayList<>();
        addOrigins(origins, allowedOrigins);
        addOrigins(origins, allowedOrigin1);
        addOrigins(origins, allowedOrigin2);
        return origins;
    }

    private void addOrigins(List<String> target, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String origin : raw.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty() && !target.contains(trimmed)) {
                target.add(trimmed);
            }
        }
    }
}
