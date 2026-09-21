package mexa.club.warehouseproject.security;

import java.util.UUID;

public record JwtAuthDetails(UUID userId, String username) {
}
