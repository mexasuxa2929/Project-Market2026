package mexa.club.gatewayservice.realtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(prefix = "app.realtime.sse", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisRealtimeListenerConfig {

    /**
     * Container konstruktori Redis ga ulanadi. @Lazy — Redis yo'q/yoniq emas bo'lsa ham gateway
     * ishga tushadi; birinchi SSE so'rovida ulanish uriniladi.
     */
    @Bean
    @Lazy
    public ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer(
            ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisMessageListenerContainer(factory);
    }
}
