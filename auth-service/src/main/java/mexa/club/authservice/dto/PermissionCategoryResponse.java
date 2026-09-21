package mexa.club.authservice.dto;

import java.util.List;

public record PermissionCategoryResponse(
        String category,
        List<PermissionResponse> permissions
) {}
