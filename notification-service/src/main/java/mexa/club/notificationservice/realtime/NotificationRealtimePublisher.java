package mexa.club.notificationservice.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Redis pub/sub orqali yangi bildirishnoma eventini yuboradi.
 * Gateway SSE controller bu eventni mobil ilovalarga yetkazadi.
 */
@Component
public class NotificationRealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationRealtimePublisher.class);
    private static final String CHANNEL = "mexa:realtime:events";

    private final StringRedisTemplate redisTemplate;

    public NotificationRealtimePublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void notificationCreated(UUID notificationId, UUID userId, String type) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("type", "NOTIFICATION_CREATED");
        payload.put("notificationId", notificationId.toString());
        payload.put("userId", userId.toString());
        payload.put("notificationType", type == null ? "" : type);
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            redisTemplate.convertAndSend(CHANNEL, json);
            log.debug("Realtime event sent: {}", json);
        } catch (Exception e) {
            log.warn("Failed to send realtime event: {}", e.getMessage());
        }
    }
}