package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class StockMinStockRequest {

    @NotNull
    private UUID productId;

    /** Ombor darajasidagi minimal stock. null berilsa override tozalanadi va mahsulot default'i ishlatiladi. */
    @Min(value = 0, message = "minStockOverride must be >= 0")
    private Integer minStockOverride;
}