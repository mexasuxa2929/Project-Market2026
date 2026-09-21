package mexa.club.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request to assign roles to a user. If roleIds is empty, ROLE_USER is assigned.")
@Getter
@Setter
@NoArgsConstructor
public class AdminUserRolesRequest {

    @Schema(
            description = "List of role UUIDs to assign. If omitted or empty, ROLE_USER is assigned.",
            example = "[\"f6169a9e-c3d1-476e-bbf1-87909ff81b50\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<@NotNull UUID> roleIds = new ArrayList<>();
}

