package mexa.club.deliveryservice.client;

import mexa.club.deliveryservice.client.payload.InternalNotificationPayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "notification-service",
        url = "${notification.service.url:http://localhost:8087}",
        contextId = "notificationDeliveryClient"
)
public interface NotificationClient {

    @PostMapping("/internal/notifications/send")
    void send(@RequestBody InternalNotificationPayload body);
}
