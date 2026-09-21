package mexa.club.warehouseproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Single item in a purchase order. Use 'unitPrice' for the cost price.")
public class PurchaseItemRequest {

    @Schema(description = "Product UUID — must exist in product-service")
    @NotNull
    private UUID productId;

    @Schema(description = "Quantity purchased", example = "50")
    @NotNull
    @DecimalMin(value = "0.0000001", inclusive = true)
    private BigDecimal quantity;

    @Schema(
            description = "Unit purchase price. NOTE: field name is 'unitPrice', not 'unitCost'",
            example = "40000.00"
    )
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal unitPrice;
}
