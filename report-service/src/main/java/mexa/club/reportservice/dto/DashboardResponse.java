package mexa.club.reportservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DashboardResponse(
        Period today,
        Month thisMonth,
        long lowStockAlerts,
        long pendingOrders
) {
    public record Period(long orders, BigDecimal revenue, long newUsers, long deliveries) {}
    public record Month(long orders, BigDecimal revenue, List<TopProduct> topProducts) {}
    public record TopProduct(UUID productId, String name, long sold) {}
}
