package mexa.club.analyticsservice.dto;

import java.util.List;
import java.util.UUID;

public record StockAlertResponse(
        int lowStockThreshold,
        List<Item> items,
        int totalLowStock
) {
    public record Item(UUID productId, String productName, long availableQuantity, UUID warehouseId) {}
}
