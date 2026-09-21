package mexa.club.analyticsservice.schedule;

import mexa.club.analyticsservice.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsScheduling {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsScheduling.class);

    private final AnalyticsService analyticsService;

    public AnalyticsScheduling(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Scheduled(fixedRate = 300_000)
    public void scheduledDailySummary() {
        try {
            analyticsService.computeDailySummaryAndPersist();
        } catch (Exception e) {
            log.error("scheduledDailySummary failed", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scheduledTopProducts() {
        try {
            analyticsService.refreshTopProducts("today");
            analyticsService.refreshTopProducts("week");
            analyticsService.refreshTopProducts("month");
        } catch (Exception e) {
            log.error("scheduledTopProducts failed", e);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledDailyRevenue() {
        try {
            analyticsService.computeAndSaveDailyRevenue();
        } catch (Exception e) {
            log.error("scheduledDailyRevenue failed", e);
        }
    }
}
