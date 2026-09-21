package mexa.club.warehouseproject.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mexa.club.warehouseproject.entity.ReturnCondition;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class StockReturnCreateRequest {
    private UUID orderId;
    @NotBlank
    @Size(max = 500)
    private String reason;
    @Size(max = 1000)
    private String note;
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
        @NotNull
        private ReturnCondition condition;
    }
}
