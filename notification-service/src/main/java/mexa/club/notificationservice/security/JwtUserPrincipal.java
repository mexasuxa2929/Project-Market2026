package mexa.club.notificationservice.security;

import java.util.UUID;

public record JwtUserPrincipal(UUID userId, String username) {}
