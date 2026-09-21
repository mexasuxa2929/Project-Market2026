package mexa.club.analyticsservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueChartResponse(String period, List<Point> points) {
    public record Point(LocalDate date, BigDecimal revenue) {}
}
