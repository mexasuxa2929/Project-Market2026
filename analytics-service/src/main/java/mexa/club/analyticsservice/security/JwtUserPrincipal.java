package mexa.club.analyticsservice.security;

import java.util.UUID;

public record JwtUserPrincipal(UUID userId, String username) {}
