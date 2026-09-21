package mexa.club.orderservice.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Redis pub/sub orqali buyurtma real-time eventlarini yuboradi.
 * Gateway SSE controller bu eventlarni mobil ilovalarga yetkazadi.
 * Barcha client'lar barcha eventni oladi — client userId bo'yicha filtrlaydi.
 */
@Component
public class OrderRealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderRealtimePublisher.class);
    private static final String CHANNEL = "mexa:realtime:events";

    private final StringRedisTemplate redisTemplate;

    public OrderRealtimePublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void orderCreated(UUID orderId, UUID userId, String orderNumber, String status) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("type", "ORDER_CREATED");
        payload.put("orderId", orderId.toString());
        payload.put("userId", userId.toString());
        payload.put("orderNumber", orderNumber == null ? "" : orderNumber);
        payload.put("orderStatus", status);
        publish(payload);
    }

    public void orderStatusChanged(UUID orderId, UUID userId, String status) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("type", "ORDER_STATUS_CHANGED");
        payload.put("orderId", orderId.toString());
        payload.put("userId", userId.toString());
        payload.put("orderStatus", status);
        publish(payload);
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
