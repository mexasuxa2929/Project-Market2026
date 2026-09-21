package mexa.club.warehouseproject.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SalesOutRequest {
    @NotNull
    private UUID orderId;

    @NotEmpty
    @Valid
    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {
        @NotNull
        private UUID productId;
        @NotNull
        @DecimalMin(value = "0.000001", inclusive = true)
        private BigDecimal quantity;
    }
}
