package mexa.club.orderservice.client.payload;

import java.util.UUID;

public record WarehouseStockLineResponse(
        UUID id,
        UUID productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity
) {}
