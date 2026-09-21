package mexa.club.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VerifyResetCodeRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Schema(name = "code", description = "Emailga yuborilgan tasdiqlash kodi")
    private String code;
}
