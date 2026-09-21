package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class StockUpsertRequest {

    @NotNull
    private UUID productId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal quantity;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal reservedQuantity;

    /** Ombordagi minimal stock override (ixtiyoriy). null bo'lsa mahsulot default'i ishlatiladi. */
    private Integer minStockOverride;
}
