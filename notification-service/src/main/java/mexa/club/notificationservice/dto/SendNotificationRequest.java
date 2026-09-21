package mexa.club.notificationservice.dto;

import jakarta.validation.constraints.NotNull;
import mexa.club.notificationservice.entity.NotificationChannel;
import mexa.club.notificationservice.entity.NotificationType;

import java.util.Map;
import java.util.UUID;

public record SendNotificationRequest(
        @NotNull UUID userId,
        @NotNull NotificationType type,
        @NotNull NotificationChannel channel,
        String recipientEmail,
        String recipientPhone,
        Map<String, Object> variables
) {}
