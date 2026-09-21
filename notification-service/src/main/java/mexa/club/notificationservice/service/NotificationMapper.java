package mexa.club.notificationservice.service;

import mexa.club.notificationservice.dto.NotificationResponse;
import mexa.club.notificationservice.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getUserId(), n.getChannel(), n.getType(), n.getRecipientEmail(), n.getRecipientPhone(),
                n.getSubject(), n.getBody(), n.getStatus(), n.getRetryCount(), n.getErrorMessage(), n.getSentAt(), n.getCreatedAt(),
                Boolean.TRUE.equals(n.getIsRead())
        );
    }
}
