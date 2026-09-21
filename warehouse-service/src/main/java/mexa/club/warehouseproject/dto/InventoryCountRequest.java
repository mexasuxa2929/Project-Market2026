package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class InventoryCountRequest {
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal countedQuantity;
}
