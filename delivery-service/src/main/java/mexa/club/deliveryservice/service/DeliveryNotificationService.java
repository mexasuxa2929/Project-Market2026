package mexa.club.deliveryservice.service;

import feign.FeignException;
import mexa.club.deliveryservice.client.NotificationClient;
import mexa.club.deliveryservice.client.OrderInternalClient;
import mexa.club.deliveryservice.client.payload.ApiResponseEnvelope;
import mexa.club.deliveryservice.client.payload.InternalNotificationPayload;
import mexa.club.deliveryservice.client.payload.OrderInternalData;
import mexa.club.deliveryservice.entity.Delivery;
import mexa.club.deliveryservice.entity.DeliveryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Yetkazib berish hodisalari bo'yicha notification-service ga async xabarlar.
 */
@Service
public class DeliveryNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryNotificationService.class);

    private final NotificationClient notificationClient;
    private final OrderInternalClient orderInternalClient;

    public DeliveryNotificationService(
            NotificationClient notificationClient,
            OrderInternalClient orderInternalClient
    ) {
        this.notificationClient = notificationClient;
        this.orderInternalClient = orderInternalClient;
    }

    @Async("deliveryNotificationExecutor")
    public void notifyAssigned(Delivery delivery, String courierName) {
        try {
            OrderCtx ctx = resolveOrder(delivery.getOrderId());
            if (ctx == null) {
                return;
            }
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("orderNumber", ctx.orderNumber());
            vars.put("courierName", courierName != null ? courierName : "");
            vars.put("trackingCode", delivery.getTrackingCode());
            send(ctx.userId(), "DELIVERY_ASSIGNED", "SMS", delivery.getRecipientPhone(), vars);
        } catch (Exception e) {
            log.warn("notifyAssigned failed for deliveryId={}: {}", delivery.getId(), e.getMessage());
        }
    }

    @Async("deliveryNotificationExecutor")
    public void notifyStatusUpdated(Delivery delivery, DeliveryStatus newStatus) {
        try {
            OrderCtx ctx = resolveOrder(delivery.getOrderId());
            if (ctx == null) {
                return;
            }
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("trackingCode", delivery.getTrackingCode());
            vars.put("status", newStatus != null ? newStatus.name() : "");
            vars.put("orderNumber", ctx.orderNumber());
            send(ctx.userId(), "DELIVERY_STATUS_UPDATED", "SMS", delivery.getRecipientPhone(), vars);
        } catch (Exception e) {
            log.warn("notifyStatusUpdated failed for deliveryId={}: {}", delivery.getId(), e.getMessage());
        }
    }

    @Async("deliveryNotificationExecutor")
    public void notifyCompleted(Delivery delivery) {
        try {
            OrderCtx ctx = resolveOrder(delivery.getOrderId());
            if (ctx == null) {
                return;
            }
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("orderNumber", ctx.orderNumber());
            vars.put("trackingCode", delivery.getTrackingCode());
            send(ctx.userId(), "DELIVERY_COMPLETED", "SMS", delivery.getRecipientPhone(), vars);
        } catch (Exception e) {
            log.warn("notifyCompleted failed for deliveryId={}: {}", delivery.getId(), e.getMessage());
        }
    }

    @Async("deliveryNotificationExecutor")
    public void notifyFailed(Delivery delivery, String reason) {
        try {
            OrderCtx ctx = resolveOrder(delivery.getOrderId());
            if (ctx == null) {
                return;
            }
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("trackingCode", delivery.getTrackingCode());
            vars.put("reason", reason != null ? reason : "");
            send(ctx.userId(), "DELIVERY_FAILED", "SMS", delivery.getRecipientPhone(), vars);
        } catch (Exception e) {
            log.warn("notifyFailed failed for deliveryId={}: {}", delivery.getId(), e.getMessage());
        }
    }

    private OrderCtx resolveOrder(UUID orderId) {
        try {
            ApiResponseEnvelope<OrderInternalData> resp = orderInternalClient.getOrder(orderId);
            if (resp == null || resp.data() == null || resp.data().userId() == null) {
                log.warn("Order {} has no userId for notifications", orderId);
                return null;
            }
            OrderInternalData d = resp.data();
            String num = d.orderNumber() != null ? d.orderNumber() : String.valueOf(orderId);
            return new OrderCtx(d.userId(), num);
        } catch (FeignException ex) {
            log.warn("Order service lookup failed for orderId={}: {}", orderId, ex.getMessage());
            return null;
        }
    }

    private void send(UUID userId, String type, String channel, String phone, Map<String, String> variables) {
        try {
            notificationClient.send(new InternalNotificationPayload(
                    userId,
                    type,
                    channel,
                    null,
                    phone,
                    variables
            ));
        } catch (FeignException ex) {
            log.warn("Notification send failed type={}: {}", type, ex.getMessage());
        }
    }

    private record OrderCtx(UUID userId, String orderNumber) {
    }
}
