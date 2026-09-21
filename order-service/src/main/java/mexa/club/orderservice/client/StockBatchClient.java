package mexa.club.orderservice.client;

import mexa.club.orderservice.client.payload.StockBatchByWarehouseResponse;
import mexa.club.orderservice.client.payload.StockBatchRequest;
import mexa.club.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Calls warehouse-service POST /api/stock/batch/by-warehouse to get, for each
 * product, how much stock is available in each warehouse. order-service uses
 * this to choose which warehouse should fulfill each order line.
 */
@FeignClient(name = "stockBatchClient", url = "${services.warehouse.url}", configuration = FeignConfig.class)
public interface StockBatchClient {
    @PostMapping("/api/stock/batch/by-warehouse")
    List<StockBatchByWarehouseResponse> getStockByWarehouse(@RequestBody StockBatchRequest request);
}
