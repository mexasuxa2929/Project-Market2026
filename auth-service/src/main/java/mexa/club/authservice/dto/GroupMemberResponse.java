package mexa.club.authservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record GroupMemberResponse(
        UUID id,
        UUID userId,
        String username,
        String email,
        String roleInGroup,
        LocalDateTime joinedAt
) {
}