package mexa.club.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InternalOrderStatsResponse(
        long orderCount,
        BigDecimal revenue,
        long paidCount,
        long cancelledCount,
        BigDecimal discountGiven,
        Map<String, Long> statusBreakdown,
        List<TopProductStat> topProductIds
) {
    public record TopProductStat(UUID productId, String productName, long count, BigDecimal revenue) {}
}
