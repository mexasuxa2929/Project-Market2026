package mexa.club.reportservice.client.payload;

import java.util.UUID;

public record WarehouseStockResponse(
        UUID productId,
        String productName,
        Integer availableQuantity
) {}
