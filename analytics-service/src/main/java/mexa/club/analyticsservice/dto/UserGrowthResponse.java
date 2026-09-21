package mexa.club.analyticsservice.dto;

import java.time.LocalDate;
import java.util.List;

public record UserGrowthResponse(String period, List<Point> points) {
    public record Point(LocalDate date, long newUsers, long totalUsers) {}
}
