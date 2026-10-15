package mexa.club.orderservice.client.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SalesOutRequestPayload(
        UUID orderId,
        List<Item> items
) {
    /**
     * @param unitPrice sotuv paytidagi haqiqiy birlik narx (OrderItem.unitPrice);
     *                  reverse/kompensatsiya chaqiruvlarida null bo'lishi mumkin.
     */
    public record Item(UUID productId, Integer quantity, BigDecimal unitPrice) {}
}
