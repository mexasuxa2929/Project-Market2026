package mexa.club.analyticsservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalOrderStatsPayload(
        long orderCount,
        BigDecimal revenue,
        long paidCount,
        long cancelledCount,
        BigDecimal discountGiven,
        Map<String, Long> statusBreakdown,
        List<TopProductStatPayload> topProductIds
) {}
