package mexa.club.analyticsservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record DiscountSummaryResponse(
        long activePromotions,
        BigDecimal discountGivenToday,
        List<TopCoupon> topCouponsLast30Days
) {
    public record TopCoupon(String code, long usageCount, BigDecimal totalDiscount) {}
}
