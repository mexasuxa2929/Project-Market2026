package mexa.club.warehouseproject.dto;

import java.util.List;
import java.util.UUID;

public record InternalWarehouseStatsResponse(
        List<LowStockItem> lowStockItems,
        int totalLowStock
) {
    public record LowStockItem(UUID productId, String productName, long availableQuantity, UUID warehouseId) {}
}
