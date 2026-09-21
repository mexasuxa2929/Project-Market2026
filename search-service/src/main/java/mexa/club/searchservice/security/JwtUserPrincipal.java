package mexa.club.searchservice.security;

import java.util.UUID;

public record JwtUserPrincipal(UUID userId, String username) {}
