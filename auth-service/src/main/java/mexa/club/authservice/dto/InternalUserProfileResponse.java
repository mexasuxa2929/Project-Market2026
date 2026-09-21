package mexa.club.authservice.dto;

import java.util.UUID;

public record InternalUserProfileResponse(
        UUID id,
        String username,
        String email
) {
}
