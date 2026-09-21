package mexa.club.orderservice.client;

import mexa.club.orderservice.client.payload.DeliveryCreatedPayload;
import mexa.club.orderservice.client.payload.InternalApiEnvelope;
import mexa.club.orderservice.client.payload.InternalDeliveryCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "deliveryClient", url = "${services.delivery.url}", configuration = mexa.club.orderservice.config.FeignConfig.class, contextId = "deliveryClient")
public interface DeliveryClient {

    @PostMapping("/internal/deliveries/create")
    InternalApiEnvelope<DeliveryCreatedPayload> createDelivery(@RequestBody InternalDeliveryCreateRequest request);
}
