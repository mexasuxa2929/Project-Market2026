package mexa.club.reportservice.client;

import mexa.club.reportservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "reportWarehouseClient", url = "${services.warehouse.url}")
public interface WarehouseServiceClient {
    @GetMapping("/api/warehouses")
    ApiResponse<Map<String, Object>> warehouses();
}
