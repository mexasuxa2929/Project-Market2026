package mexa.club.orderservice.client;

import mexa.club.orderservice.config.FeignConfig;
import mexa.club.orderservice.client.payload.PriceResolveResponse;
import mexa.club.orderservice.client.payload.ProductResponse;
import mexa.club.orderservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "productServiceClient", url = "${services.product.url}", configuration = FeignConfig.class)
public interface ProductServiceClient {
    @GetMapping("/api/products/{id}")
    ApiResponse<ProductResponse> getProduct(@PathVariable("id") UUID id);

    /** Miqdorga bog'liq yagona narx: liniyalar (tiers) yoki muddatli chegirma bilan resolve qilinadi. */
    @GetMapping("/api/products/{id}/price")
    ApiResponse<PriceResolveResponse> resolvePrice(@PathVariable("id") UUID id, @RequestParam("qty") int qty);
}
