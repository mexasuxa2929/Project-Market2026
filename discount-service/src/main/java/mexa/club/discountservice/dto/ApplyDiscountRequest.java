package mexa.club.discountservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ApplyDiscountRequest(
        @NotBlank String code,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "orderAmount must be positive") BigDecimal orderAmount,
        List<UUID> productIds,
        List<UUID> categoryIds
) {
    public ApplyDiscountRequest {
        productIds = productIds == null ? List.of() : List.copyOf(productIds);
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
    }
}
