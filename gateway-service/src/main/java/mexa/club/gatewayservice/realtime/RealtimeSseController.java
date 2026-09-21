package mexa.club.gatewayservice.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;


@RestController
@ConditionalOnProperty(prefix = "app.realtime.sse", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(ReactiveRedisMessageListenerContainer.class)
public class RealtimeSseController {

    private static final Logger log = LoggerFactory.getLogger(RealtimeSseController.class);
    private static final int MAX_SSE_CONNECTIONS = 100;

    private final ReactiveRedisMessageListenerContainer container;
    private final String channel;
    private final Semaphore semaphore = new Semaphore(MAX_SSE_CONNECTIONS);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public RealtimeSseController(
            @Lazy ReactiveRedisMessageListenerContainer container,
            @Value("${app.realtime.channel:mexa:realtime:events}") String channel) {
        this.container = container;
        this.channel = channel;
    }

    @GetMapping(value = "/api/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream() {
        if (!semaphore.tryAcquire()) {
            log.warn("SSE connection limit reached ({}/{}), rejecting request", activeConnections.get(), MAX_SSE_CONNECTIONS);
            return Flux.error(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "SSE connection limit reached"));
        }
        activeConnections.incrementAndGet();
        log.debug("SSE connection opened (active: {})", activeConnections.get());

        return container.receive(new ChannelTopic(channel))
                .map(RealtimeSseController::toSse)
                .doOnComplete(() -> {
                    activeConnections.decrementAndGet();
                    semaphore.release();
                    log.debug("SSE connection closed (active: {})", activeConnections.get());
                })
                .doOnError(e -> {
                    activeConnections.decrementAndGet();
                    semaphore.release();
                    log.debug("SSE connection error (active: {})", activeConnections.get());
                });
    }

    private static ServerSentEvent<String> toSse(ReactiveSubscription.Message<String, String> message) {
        String body = message.getMessage();
        return ServerSentEvent.builder(body != null ? body : "").build();
    }
}
