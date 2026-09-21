package mexa.club.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Admin: OTP siz to'g'ridan-to'g'ri user yaratish (faqat SUPER_ADMIN)")
public class AdminCreateUserRequest {

    @NotBlank
    @Size(max = 128)
    @Schema(description = "Noyob login", example = "newuser")
    private String username;

    @NotBlank
    @Email
    @Schema(description = "Noyob email", example = "user@example.com")
    private String email;

    @NotBlank
    @Size(min = 8, max = 128)
    @Schema(description = "Xavfsiz parol", example = "password12")
    private String password;

    @Schema(
            description = "Rol UUID lari (GET /api/admin/roles). Bo'sh yoki null bo'lsa — faqat ROLE_USER beriladi.",
            example = "[]"
    )
    private List<UUID> roleIds;

    @Schema(description = "Aktivlik (default: true)", defaultValue = "true")
    private boolean enabled = true;
}
