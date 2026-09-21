package mexa.club.discountservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InternalDiscountApplyRequest(
        UUID orderId,
        @NotNull UUID userId,
        @NotBlank String code,
        @NotNull BigDecimal orderAmount,
        List<UUID> productIds,
        List<UUID> categoryIds
) {
    public InternalDiscountApplyRequest {
        productIds = productIds == null ? List.of() : List.copyOf(productIds);
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
    }
}
