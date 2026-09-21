package mexa.club.paymentservice.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull Long userId,
        @NotNull @Min(1) Long amountTiyin,
        @NotBlank String returnUrl,
        UUID orderServiceId
) {}
