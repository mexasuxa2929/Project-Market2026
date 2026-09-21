package mexa.club.shopservice.security;

import java.util.UUID;

public record JwtUserPrincipal(UUID userId, String username) {
}
