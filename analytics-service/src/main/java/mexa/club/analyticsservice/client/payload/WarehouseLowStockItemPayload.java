package mexa.club.analyticsservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WarehouseLowStockItemPayload(
        UUID productId,
        String productName,
        long availableQuantity,
        UUID warehouseId
) {}
