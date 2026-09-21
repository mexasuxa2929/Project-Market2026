package mexa.club.orderservice.client;

import mexa.club.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "notificationClient", url = "${services.notification.url}", configuration = FeignConfig.class)
public interface NotificationClient {
    @PostMapping("/internal/notifications/order")
    ResponseEntity<Void> sendOrderEvent(@RequestBody Map<String, Object> payload);
}
