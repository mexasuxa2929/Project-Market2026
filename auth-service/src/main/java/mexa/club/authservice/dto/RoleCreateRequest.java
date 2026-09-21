package mexa.club.authservice.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

public record RoleCreateRequest(
        @NotBlank String name,
        String displayName,
        String description,
        Set<String> permissionNames
) {
    public RoleCreateRequest {
        if (permissionNames == null) {
            permissionNames = new HashSet<>();
        }
    }
}
