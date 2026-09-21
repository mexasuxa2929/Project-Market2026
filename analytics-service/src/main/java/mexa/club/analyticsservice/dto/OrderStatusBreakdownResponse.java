package mexa.club.analyticsservice.dto;

import java.util.Map;

public record OrderStatusBreakdownResponse(
        String period,
        Map<String, Long> breakdown,
        double conversionRate
) {}
