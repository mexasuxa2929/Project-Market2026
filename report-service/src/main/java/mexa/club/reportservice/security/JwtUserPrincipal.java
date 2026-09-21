package mexa.club.reportservice.security;

import java.util.UUID;

public record JwtUserPrincipal(UUID userId, String username) {}
