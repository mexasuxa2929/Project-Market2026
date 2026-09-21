package mexa.club.deliveryservice.dto;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record DeliveryFeeUpdateRequest(
        @PositiveOrZero BigDecimal fee
) {}
