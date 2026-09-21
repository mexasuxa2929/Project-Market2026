package mexa.club.orderservice.client.payload;

import java.util.List;
import java.util.UUID;

public record SalesOutRequestPayload(
        UUID orderId,
        List<Item> items
) {
    public record Item(UUID productId, Integer quantity) {}
}
