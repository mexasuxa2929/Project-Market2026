package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.OperatingExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatingExpenseRequest {
    @NotNull
    private OperatingExpenseCategory category;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String currency;

    private String description;

    private LocalDateTime incurredAt;
}
