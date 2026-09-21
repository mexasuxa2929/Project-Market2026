package mexa.club.discountservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalDiscountConfirmRequest(
        UUID orderId,
        @NotNull UUID userId,
        @NotBlank String code,
        @NotNull BigDecimal discountAmount
) {}
