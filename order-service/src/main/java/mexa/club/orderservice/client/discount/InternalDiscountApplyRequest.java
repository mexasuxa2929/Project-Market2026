package mexa.club.orderservice.client.discount;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InternalDiscountApplyRequest(
        UUID orderId,
        UUID userId,
        String code,
        BigDecimal orderAmount,
        List<UUID> productIds,
        List<UUID> categoryIds
) {
    public InternalDiscountApplyRequest {
        productIds = productIds == null ? List.of() : List.copyOf(productIds);
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
    }
}
