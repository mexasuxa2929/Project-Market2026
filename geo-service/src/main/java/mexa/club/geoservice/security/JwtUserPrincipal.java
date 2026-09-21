package mexa.club.geoservice.security;

import java.util.UUID;

public record JwtUserPrincipal(UUID userId, String username) {}
