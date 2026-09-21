package mexa.club.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GoogleAuthRequest {

    @NotBlank
    private String idToken;

    /** Mijoz qurilma identifikatori (refresh tokenni qurilmaga bog'lash uchun). Ixtiyoriy. */
    private String deviceId;
}
