package mexa.club.shopservice.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemPayload(
        UUID id,
        UUID productId,
        String productName,
        String imageUrl,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
