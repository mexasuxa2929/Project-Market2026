package mexa.club.notificationservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkNotificationRequest(
        @NotEmpty List<@Valid SendNotificationRequest> notifications
) {}
