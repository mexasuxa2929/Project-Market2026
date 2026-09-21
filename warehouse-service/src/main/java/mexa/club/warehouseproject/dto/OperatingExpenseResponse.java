package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.OperatingExpense;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatingExpenseResponse {
    private UUID id;
    private UUID warehouseId;
    private OperatingExpenseCategoryDto category;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDateTime incurredAt;
    private UUID createdBy;
    private LocalDateTime createdAt;

    public enum OperatingExpenseCategoryDto {
        WAREHOUSE_RENT, EMPLOYEE_SALARY, ELECTRICITY, SECURITY, MAINTENANCE, ADMINISTRATIVE, OTHER
    }

    public static OperatingExpenseResponse fromEntity(OperatingExpense e) {
        return OperatingExpenseResponse.builder()
                .id(e.getId())
                .warehouseId(e.getWarehouse() != null ? e.getWarehouse().getId() : null)
                .category(OperatingExpenseCategoryDto.valueOf(e.getCategory().name()))
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .description(e.getDescription())
                .incurredAt(e.getIncurredAt())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
