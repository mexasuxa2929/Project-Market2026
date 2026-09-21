package mexa.club.orderservice.client;

import mexa.club.orderservice.config.FeignConfig;
import mexa.club.orderservice.client.payload.SalesOutRequestPayload;
import mexa.club.orderservice.client.payload.WarehouseStockLineResponse;
import mexa.club.orderservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "warehouseStockClient", url = "${services.warehouse.url}", configuration = FeignConfig.class)
public interface WarehouseStockClient {
    @GetMapping("/api/warehouses/{warehouseId}/stock/lines/{productId}")
    ApiResponse<WarehouseStockLineResponse> getStockLine(
            @PathVariable("warehouseId") UUID warehouseId,
            @PathVariable("productId") UUID productId
    );

    @PostMapping("/api/warehouses/{warehouseId}/stock/sales-out")
    ApiResponse<Object> salesOut(@PathVariable("warehouseId") UUID warehouseId, @RequestBody SalesOutRequestPayload payload);

    @PostMapping("/api/warehouses/{warehouseId}/stock/sales-out/reverse")
    ApiResponse<Object> reverseSalesOut(@PathVariable("warehouseId") UUID warehouseId, @RequestBody SalesOutRequestPayload payload);
}
