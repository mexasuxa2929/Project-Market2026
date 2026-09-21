package mexa.club.notificationservice.dto;

import mexa.club.notificationservice.entity.NotificationChannel;
import mexa.club.notificationservice.entity.NotificationStatus;
import mexa.club.notificationservice.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationChannel channel,
        NotificationType type,
        String recipientEmail,
        String recipientPhone,
        String subject,
        String body,
        NotificationStatus status,
        Integer retryCount,
        String errorMessage,
        LocalDateTime sentAt,
        LocalDateTime createdAt,
        Boolean read
) {}
