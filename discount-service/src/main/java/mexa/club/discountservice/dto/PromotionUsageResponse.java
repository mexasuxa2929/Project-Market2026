package mexa.club.discountservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PromotionUsageResponse(
        UUID id,
        UUID promotionId,
        UUID userId,
        UUID orderId,
        BigDecimal discountAmount,
        LocalDateTime usedAt
) {}
