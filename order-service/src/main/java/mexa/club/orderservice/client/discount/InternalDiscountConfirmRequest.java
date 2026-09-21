package mexa.club.orderservice.client.discount;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalDiscountConfirmRequest(
        UUID orderId,
        UUID userId,
        String code,
        BigDecimal discountAmount
) {}
