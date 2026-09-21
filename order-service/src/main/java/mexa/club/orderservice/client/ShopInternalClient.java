package mexa.club.orderservice.client;

import mexa.club.orderservice.client.payload.InternalAddressPayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "shopInternalClient", url = "${services.shop.url}", configuration = mexa.club.orderservice.config.FeignConfig.class, contextId = "shopInternalClient")
public interface ShopInternalClient {

    @GetMapping("/api/shops/internal/addresses/{id}")
    InternalAddressPayload getAddress(@PathVariable("id") UUID id);
}
