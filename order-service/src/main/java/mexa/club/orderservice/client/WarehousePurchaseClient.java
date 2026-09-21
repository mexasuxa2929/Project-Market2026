package mexa.club.orderservice.client;

import mexa.club.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Warehouse-service internal auto-purchase: buyurtmada zaxira yetmaganda
 * omborga avtomatik zakaz. X-Internal-Secret FeignConfig orqali avtomatik
 * qo'shiladi (/internal/ prefiksi uchun).
 */
@FeignClient(name = "warehousePurchaseClient", url = "${services.warehouse.url}", configuration = FeignConfig.class)
public interface WarehousePurchaseClient {

    @PostMapping("/internal/warehouses/{warehouseId}/auto-purchase")
    AutoPurchaseResponse autoPurchase(
            @PathVariable("warehouseId") UUID warehouseId,
            @RequestBody AutoPurchaseRequest request
    );

    record AutoPurchaseItem(UUID productId, BigDecimal quantity) {}

    record AutoPurchaseRequest(String orderNumber, List<AutoPurchaseItem> items) {}

    record AutoPurchaseResponse(UUID purchaseId, String invoiceNumber, int itemCount) {}
}
