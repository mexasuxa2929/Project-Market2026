package mexa.club.productservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ProductActiveRequest {

    /** Agar null bo'lsa, server joriy holatni invert qiladi (toggle). */
    private Boolean active;
}

