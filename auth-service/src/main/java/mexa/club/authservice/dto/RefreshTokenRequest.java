package mexa.club.authservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenRequest {
    private String refreshToken;

    /** Mijoz qurilma identifikatori (refresh tokenni qurilmaga bog'lash uchun). Ixtiyoriy. */
    private String deviceId;
}
