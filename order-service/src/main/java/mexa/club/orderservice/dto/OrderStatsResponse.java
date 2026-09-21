package mexa.club.orderservice.dto;

import java.math.BigDecimal;
import java.util.Map;

public record OrderStatsResponse(
        Totals today,
        Totals thisMonth,
        Map<String, Long> byStatus,
        long pendingCount
) {
    public record Totals(long total, BigDecimal revenue) {}
}
