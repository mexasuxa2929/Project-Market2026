package mexa.club.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String displayName,
        String description,
        @JsonProperty("isSystem") boolean isSystem,
        List<String> permissions,
        LocalDateTime createdAt,
        String createdBy
) {}
