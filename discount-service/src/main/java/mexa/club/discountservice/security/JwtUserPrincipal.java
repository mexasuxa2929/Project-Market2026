package mexa.club.discountservice.security;

import java.util.UUID;

public record JwtUserPrincipal(UUID userId, String username) {}
