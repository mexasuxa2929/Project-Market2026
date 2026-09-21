package mexa.club.reportservice.client.payload;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        LocalDateTime createdAt
) {}
