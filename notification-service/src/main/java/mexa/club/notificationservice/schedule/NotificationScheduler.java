package mexa.club.notificationservice.schedule;

import mexa.club.notificationservice.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {
    private final NotificationService notificationService;

    public NotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 30000)
    public void processQueue() {
        notificationService.processPending();
        notificationService.moveOldFailedToDeadLetter();
    }
}
