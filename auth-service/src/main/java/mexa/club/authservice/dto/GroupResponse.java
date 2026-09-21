package mexa.club.authservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String description,
        int memberCount,
        String createdBy,
        LocalDateTime createdAt
) {
}