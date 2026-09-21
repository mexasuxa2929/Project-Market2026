package mexa.club.orderservice.client.payload;

import java.util.UUID;

/** auth-service /internal/auth/users/{id}/profile javobi. */
public record UserProfilePayload(
        UUID id,
        String username,
        String email
) {
}
