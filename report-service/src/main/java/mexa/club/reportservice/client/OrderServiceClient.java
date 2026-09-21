package mexa.club.reportservice.client;

import mexa.club.reportservice.client.payload.OrderResponse;
import mexa.club.reportservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "reportOrderClient", url = "${services.order.url}")
public interface OrderServiceClient {
    @GetMapping("/api/admin/orders")
    ApiResponse<java.util.Map<String, Object>> all(
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );
}
