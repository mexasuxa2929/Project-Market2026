package mexa.club.analyticsservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record DashboardSummaryResponse(
        Instant computedAt,
        long ordersToday,
        BigDecimal revenueToday,
        long ordersWeek,
        BigDecimal revenueWeek,
        long totalUsers,
        long newUsersToday,
        long verifiedUsers,
        int lowStockTotal,
        long activePromotions,
        BigDecimal discountGivenToday,
        Map<String, Long> statusBreakdownToday
) {}
