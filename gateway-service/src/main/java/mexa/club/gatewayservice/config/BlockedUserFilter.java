package mexa.club.gatewayservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Har bir so'rovda JWT token'dan userId olib, Redis'da blocked:{userId} borligini tekshiradi.
 * Blocklangan user darhol 401 qaytariladi.
 */
@Component
public class BlockedUserFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(BlockedUserFilter.class);

    private final StringRedisTemplate redisTemplate;

    public BlockedUserFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return chain.filter(exchange);
        }

        String userId = extractUserId(token);
        if (userId == null) {
            return chain.filter(exchange);
        }

        try {
            String blocked = redisTemplate.opsForValue().get("blocked:" + userId);
            if ("1".equals(blocked)) {
                log.info("Blocked user attempted access: userId={}", userId);
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                String body = "{\"success\":false,\"error\":{\"code\":\"ACCOUNT_DISABLED\",\"message\":\"Hisob bloklangan. Admin bilan bog'laning.\"}}";
                return response.writeWith(Mono.just(
                        response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
            }
        } catch (Exception e) {
            log.warn("Redis check failed for user {}: {}", userId, e.getMessage());
        }

        return chain.filter(exchange);
    }

    private String extractUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] getSigningKey() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = "mexa_jwt_2024_secret_key_32chars_long!!";
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
