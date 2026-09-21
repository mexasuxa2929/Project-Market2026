package mexa.club.discountservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PromotionStatsResponse(
        long totalPromotions,
        BigDecimal totalDiscountAmountGiven,
        UUID topPromotionId,
        String topPromotionCode,
        long topPromotionUsageCount
) {}
