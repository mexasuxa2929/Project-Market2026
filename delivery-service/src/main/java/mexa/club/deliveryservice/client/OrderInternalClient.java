package mexa.club.deliveryservice.client;

import mexa.club.deliveryservice.client.payload.ApiResponseEnvelope;
import mexa.club.deliveryservice.client.payload.OrderInternalData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(
        name = "order-service",
        url = "${order.service.url:http://localhost:8085}",
        contextId = "orderInternalClient"
)
public interface OrderInternalClient {

    @GetMapping("/internal/orders/{id}")
    ApiResponseEnvelope<OrderInternalData> getOrder(@PathVariable("id") java.util.UUID id);

    @PostMapping("/internal/orders/{id}/delivered")
    ApiResponseEnvelope<OrderInternalData> markDelivered(@PathVariable("id") UUID id);
}
