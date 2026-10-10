package mexa.club.authservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record AssignRoleRequest(
        @NotNull(message = "userId is required")
        UUID userId,
        @NotEmpty(message = "roleIds must not be empty")
        Set<UUID> roleIds
) {}
