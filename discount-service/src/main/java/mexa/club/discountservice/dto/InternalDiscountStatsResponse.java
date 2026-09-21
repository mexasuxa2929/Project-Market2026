package mexa.club.discountservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record InternalDiscountStatsResponse(
        long activePromotions,
        BigDecimal totalDiscountGiven,
        List<TopPromotionStat> topPromotions
) {
    public record TopPromotionStat(String code, long usageCount, BigDecimal totalDiscount) {}
}
