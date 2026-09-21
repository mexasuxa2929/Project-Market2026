package mexa.club.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductPriceTierRequest {

    @Min(value = 0, message = "Minimal miqdor 0 dan kam bo'lmasligi kerak")
    private int minQty;

    /** null = cheksiz */
    private Integer maxQty;

    @NotNull(message = "Narx majburiy")
    @DecimalMin(value = "0.0", inclusive = true, message = "Narx manfiy bo'lmasligi kerak")
    private BigDecimal price;

    private String currency = "UZS";

    private String priceType = "RETAIL";
}
