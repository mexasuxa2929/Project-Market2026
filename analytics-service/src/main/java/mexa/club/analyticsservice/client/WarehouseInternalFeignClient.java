package mexa.club.analyticsservice.client;

import mexa.club.analyticsservice.client.payload.InternalWarehouseStatsPayload;
import mexa.club.analyticsservice.config.WarehouseFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "warehouseInternal",
        url = "${app.services.warehouse-url}",
        configuration = WarehouseFeignConfig.class
)
public interface WarehouseInternalFeignClient {

    @GetMapping("/internal/warehouse/stats")
    InternalWarehouseStatsPayload stats(@RequestParam("threshold") int threshold);
}
