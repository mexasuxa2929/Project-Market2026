package mexa.club.discountservice.client;

import mexa.club.discountservice.client.dto.ProductCatalogPayload;
import mexa.club.discountservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "discountProductCatalogClient", url = "${services.product.url}")
public interface ProductCatalogClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductCatalogPayload> getProduct(@PathVariable("id") UUID id);
}
