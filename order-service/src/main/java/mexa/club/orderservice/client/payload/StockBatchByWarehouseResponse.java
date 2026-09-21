package mexa.club.orderservice.client.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Mirrors warehouse-service's per-warehouse stock breakdown for a product.
 * Used by order-service to pick which warehouse should fulfill each order item.
 */
public record StockBatchByWarehouseResponse(
        UUID productId,
        List<WarehouseBreakdown> warehouses
) {
    public record WarehouseBreakdown(
            UUID warehouseId,
            String warehouseName,
            BigDecimal quantity,
            BigDecimal reservedQuantity,
            BigDecimal availableQuantity
    ) {}
}
