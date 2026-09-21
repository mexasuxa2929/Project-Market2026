package mexa.club.orderservice.security;

import java.util.UUID;

public record JwtUserPrincipal(UUID userId, String username) {}
