package mexa.club.authservice.dto;

import java.util.Set;
import java.util.UUID;

public record AssignRoleRequest(
        UUID userId,
        Set<UUID> roleIds
) {}
