package mexa.club.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bitta rasmning 3 variant URL'i — original, thumb, medium.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageVariants {
    private String original;
    private String thumb;
    private String medium;
}
