package mexa.club.deliveryservice.security;

import java.util.UUID;

public record JwtUserPrincipal(UUID userId, String username) {}
