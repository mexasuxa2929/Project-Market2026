package mexa.club.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record DeliveryZoneRequest(
        @NotBlank String region,
        @NotBlank String district,
        @NotNull @PositiveOrZero BigDecimal fee,
        @NotNull Integer estimatedDays
) {}
