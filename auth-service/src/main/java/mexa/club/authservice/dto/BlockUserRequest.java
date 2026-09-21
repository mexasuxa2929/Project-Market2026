package mexa.club.authservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BlockUserRequest {

    /** Bloklash sababi (ixtiyoriy, lekin tavsiya etiladi). */
    private String reason;
}
