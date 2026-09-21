package mexa.club.deliveryservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TrackingEventResponse(
        UUID id,
        String status,
        String location,
        String description,
        LocalDateTime createdAt
) {}
