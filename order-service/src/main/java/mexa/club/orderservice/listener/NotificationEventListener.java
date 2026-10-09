package mexa.club.orderservice.listener;

import mexa.club.orderservice.OrderCreatedEvent;
import mexa.club.orderservice.client.NotificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

import java.util.Map;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationClient notificationClient;

    public NotificationEventListener(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Async("orderNotificationExecutor")
    @EventListener
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            notificationClient.sendOrderEvent(Map.of(
                    "userId", event.userId(),
                    "type", "ORDER_CREATED",
                    "channel", "PUSH",
                    "variables", Map.of(
                            "orderId", String.valueOf(event.orderId()),
                            "orderNumber", event.orderNumber(),
                            "totalAmount", String.valueOf(event.totalAmount())
                    )
            ));
        } catch (Exception ex) {
            log.error("Order notification failed for orderId={}", event.orderId(), ex);
        }
    }
}
