package mexa.club.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminUserUpdateRequest {

    @Size(min = 3, max = 64)
    private String username;

    @Email
    private String email;

    private Boolean enabled;
}

