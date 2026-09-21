package mexa.club.orderservice.client.payload;

import java.util.List;
import java.util.UUID;

public record StockBatchRequest(List<UUID> productIds) {}
