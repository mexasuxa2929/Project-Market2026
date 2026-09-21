package mexa.club.productservice.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis pub/sub orqali real-time event yuboradi.
 * Gateway SSE controller bu eventlarni mobil ilovalarga yetkazadi.
 */
@Component
public class RealtimeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventPublisher.class);
    private static final String CHANNEL = "mexa:realtime:events";

    private final StringRedisTemplate redisTemplate;

    public RealtimeEventPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Mahsulot o'chirilganda event yuboradi.
     * @param productId o'chirilgan mahsulot ID si
     */
    public void productDeleted(java.util.UUID productId) {
        publish(Map.of(
                "type", "PRODUCT_DELETED",
                "productId", productId.toString()
        ));
    }

    /**
     * Mahsulot yangilanganda event yuboradi.
     * @param productId yangilangan mahsulot ID si
     */
    public void productUpdated(java.util.UUID productId) {
        publish(Map.of(
                "type", "PRODUCT_UPDATED",
                "productId", productId.toString()
        ));
    }

    /**
     * Mahsulot yaratilganda event yuboradi.
     * @param productId yaratilgan mahsulot ID si
     */
    public void productCreated(java.util.UUID productId) {
        publish(Map.of(
                "type", "PRODUCT_CREATED",
                "productId", productId.toString()
        ));
    }

    private void publish(Map<String, String> payload) {
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            redisTemplate.convertAndSend(CHANNEL, json);
            log.debug("Realtime event sent: {}", json);
        } catch (Exception e) {
            log.warn("Failed to send realtime event: {}", e.getMessage());
        }
    }
}
