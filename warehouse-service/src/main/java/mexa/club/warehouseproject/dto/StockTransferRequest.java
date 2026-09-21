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
public class StockTransferRequest {

    @NotNull
    private UUID toWarehouseId;

    @NotNull
    private UUID productId;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal quantity;
}
