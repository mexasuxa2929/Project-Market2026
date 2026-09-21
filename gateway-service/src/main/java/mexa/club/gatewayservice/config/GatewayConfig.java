package mexa.club.gatewayservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> Mono.just(resolveKey(exchange));
    }

    private String resolveKey(ServerWebExchange exchange) {
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return "jwt:" + sha256(auth.substring(7).trim());
        }
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return "ip:" + ip;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return new String(Hex.encode(hash));
        } catch (Exception ex) {
            return "invalid";
        }
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.rate-limiter.enabled", havingValue = "false", matchIfMissing = false)
    public RateLimiter noOpRateLimiter(ConfigurationService configurationService) {
        return new NoOpRateLimiter(configurationService);
    }

    @Bean
    @ConditionalOnProperty(name = "app.rate-limiter.enabled", havingValue = "true", matchIfMissing = false)
    public RedisScript<List<Long>> requestRateLimiterScript() {
        DefaultRedisScript<List<Long>> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("META-INF/scripts/request_rate_limiter.lua")));
        @SuppressWarnings("unchecked")
        Class<List<Long>> resultType = (Class<List<Long>>) (Class<?>) List.class;
        script.setResultType(resultType);
        return script;
    }

    @Bean
    @ConditionalOnProperty(name = "app.rate-limiter.enabled", havingValue = "true", matchIfMissing = false)
    public RedisRateLimiter redisRateLimiter(
            ReactiveStringRedisTemplate redisTemplate,
            RedisScript<List<Long>> requestRateLimiterScript,
            ConfigurationService configurationService) {
        return new RedisRateLimiter(redisTemplate, requestRateLimiterScript, configurationService);
    }

    private static class NoOpRateLimiter extends org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter<RedisRateLimiter.Config> {

        protected NoOpRateLimiter(ConfigurationService configurationService) {
            super(RedisRateLimiter.Config.class, RedisRateLimiter.CONFIGURATION_PROPERTY_NAME, configurationService);
        }

        @Override
        public Mono<Response> isAllowed(String routeId, String id) {
            return Mono.just(new RateLimiter.Response(true, Collections.emptyMap()));
        }
    }
}
