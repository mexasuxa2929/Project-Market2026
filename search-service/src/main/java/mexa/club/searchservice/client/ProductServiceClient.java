package mexa.club.searchservice.client;

import mexa.club.searchservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "searchProductClient", url = "${services.product.url}")
public interface ProductServiceClient {
    @GetMapping("/api/products")
    ApiResponse<Map<String, Object>> products(@RequestParam("page") int page, @RequestParam("size") int size);
}
