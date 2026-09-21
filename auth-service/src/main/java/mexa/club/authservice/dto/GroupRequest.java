package mexa.club.authservice.dto;

import jakarta.annotation.Nullable;

public record GroupRequest(
        String name,
        @Nullable String description
) {
}