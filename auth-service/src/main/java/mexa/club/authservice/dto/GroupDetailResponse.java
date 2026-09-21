package mexa.club.authservice.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GroupDetailResponse(
        UUID id,
        String name,
        String description,
        int memberCount,
        String createdBy,
        LocalDateTime createdAt,
        List<GroupMemberResponse> members
) {
}