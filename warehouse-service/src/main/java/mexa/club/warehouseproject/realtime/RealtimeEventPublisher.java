package mexa.club.warehouseproject.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RealtimeEventPublisher {

    private static final String SERVICE = "warehouse-service";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String channel;

    public RealtimeEventPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.realtime.channel:mexa:realtime:events}") String channel
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.channel = channel;
    }

    public void publishMutation(String method, String path) {
        try {
            String json = objectMapper.writeValueAsString(
                    Map.of("method", method, "path", path, "service", SERVICE));
            redisTemplate.convertAndSend(channel, json);
        } catch (Exception ignored) {
            // Redis down or serialization failure — must not affect the main transaction
        }
    }
}
