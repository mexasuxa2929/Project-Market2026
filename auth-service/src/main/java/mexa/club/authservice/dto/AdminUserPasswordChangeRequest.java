package mexa.club.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminUserPasswordChangeRequest {

    @NotBlank
    @Size(min = 8, max = 128)
    private String oldPassword;

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword;
}

