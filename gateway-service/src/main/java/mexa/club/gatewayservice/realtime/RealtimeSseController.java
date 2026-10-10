package mexa.club.gatewayservice.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Gateway SSE endpointi.
 *
 * Xavfsizlik:
 *  - Endpoint o'zi authenticated (SecurityConfig: /api/realtime/** → JWT).
 *  - Har bir event faqat o'z egasiga yuboriladi: payload'da userId bo'lsa
 *    faqat o'sha user (yoki ORDER_VIEW ruxsatiga ega admin) oladi.
 *  - Global limit (app.realtime.sse.max-connections) + user boshiga limit
 *    (app.realtime.sse.per-user-limit).
 */
@RestController
@ConditionalOnProperty(prefix = "app.realtime.sse", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RealtimeSseController {

    private static final Logger log = LoggerFactory.getLogger(RealtimeSseController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReactiveRedisMessageListenerContainer container;
    private final String channel;
    private final int maxConnections;
    private final int perUserLimit;
    private final Semaphore semaphore;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Integer> perUserActive = new ConcurrentHashMap<>();

    public RealtimeSseController(
            @Lazy ReactiveRedisMessageListenerContainer container,
            @Value("${app.realtime.channel:mexa:realtime:events}") String channel,
            @Value("${app.realtime.sse.max-connections:100}") int maxConnections,
            @Value("${app.realtime.sse.per-user-limit:5}") int perUserLimit) {
        this.container = container;
        this.channel = channel;
        this.maxConnections = maxConnections;
        this.perUserLimit = perUserLimit;
        this.semaphore = new Semaphore(maxConnections);
    }

    @GetMapping(value = "/api/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(RealtimeSseController::toJwt)
                .flatMapMany(jwt -> jwt == null
                        ? Flux.<ServerSentEvent<String>>error(new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required"))
                        : openStream(jwt))
                .switchIfEmpty(Flux.error(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required")));
    }

    private static Jwt toJwt(java.security.Principal principal) {
        if (principal instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken token) {
            return token.getToken();
        }
        if (principal instanceof Jwt j) {
            return j;
        }
        return null;
    }

    private Flux<ServerSentEvent<String>> openStream(Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        if (userId == null || userId.isBlank()) {
            userId = jwt.getSubject();
        }
        String currentUserId = userId;
        boolean admin = isEventAdmin(jwt);

        if (!acquireUser(currentUserId)) {
            log.warn("SSE per-user limit reached for user {} ({}/{})", currentUserId,
                    perUserActive.getOrDefault(currentUserId, 0), perUserLimit);
            return Flux.error(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "SSE per-user connection limit reached"));
        }
        if (!semaphore.tryAcquire()) {
            releaseUser(currentUserId);
            log.warn("SSE connection limit reached ({}/{}, user {})", activeConnections.get(), maxConnections, maskUserId(currentUserId));
            return Flux.error(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "SSE connection limit reached"));
        }
        activeConnections.incrementAndGet();
        log.debug("SSE connection opened for user {} (active: {})", maskUserId(currentUserId), activeConnections.get());

        return container.receive(new ChannelTopic(channel))
                .filter(msg -> allowed(msg.getMessage(), currentUserId, admin))
                .map(msg -> toSse(msg.getMessage()))
                .doFinally(signal -> {
                    activeConnections.decrementAndGet();
                    semaphore.release();
                    releaseUser(currentUserId);
                    log.debug("SSE connection closed for user {} (active: {})", maskUserId(currentUserId), activeConnections.get());
                });
    }

    /**
     * Payload'da userId/recipientId bo'lsa — faqat o'z egasi yoki admin
     * (ROLE_ADMIN/ROLE_SUPER_ADMIN yoki ORDER_MANAGE ruxsati) ko'radi.
     * Oddiy foydalanuvchida ORDER_VIEW bor, u boshqalar buyurtmasini
     * ko'ra olmaydi. userId'siz eventlar (PRODUCT_UPDATED va h.k.) hammaqa.
     */
    private boolean allowed(String body, String currentUserId, boolean admin) {
        if (body == null || body.isBlank()) {
            return false;
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            String owner = firstText(node, "userId", "recipientId");
            if (owner == null) {
                return true;
            }
            return owner.equals(currentUserId) || admin;
        } catch (Exception parseError) {
            // JSON emas — xavfsiz tomonda qoldirilmaydi
            return false;
        }
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode v = node.get(field);
            if (v != null && v.isTextual() && !v.asText().isBlank()) {
                return v.asText();
            }
        }
        return null;
    }

    private static boolean isEventAdmin(Jwt jwt) {
        List<String> perms = jwt.getClaimAsStringList("permissions");
        if (perms != null && (perms.contains("ORDER_MANAGE") || perms.contains("AUDIT_LOG_VIEW"))) {
            return true;
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN"));
    }

    private boolean acquireUser(String userId) {
        if (userId == null) {
            return false;
        }
        boolean[] ok = {false};
        perUserActive.compute(userId, (k, count) -> {
            int next = count == null ? 1 : count + 1;
            if (next <= perUserLimit) {
                ok[0] = true;
                return next;
            }
            return count;
        });
        return ok[0];
    }

    private void releaseUser(String userId) {
        if (userId == null) {
            return;
        }
        perUserActive.computeIfPresent(userId, (k, count) -> count <= 1 ? null : count - 1);
    }

    private static ServerSentEvent<String> toSse(String body) {
        return ServerSentEvent.builder(Objects.requireNonNullElse(body, "")).build();
    }

    // O-62: log'da to'liq userId/PII emas — qisqartirilgan ko'rinish
    private static String maskUserId(String id) {
        if (id == null || id.isBlank()) return "-";
        if (id.length() <= 10) return "***";
        return id.substring(0, 6) + "..." + id.substring(id.length() - 4);
    }

}
