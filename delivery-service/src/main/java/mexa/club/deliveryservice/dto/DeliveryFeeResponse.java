package mexa.club.deliveryservice.dto;

import java.math.BigDecimal;

public record DeliveryFeeResponse(BigDecimal fee, Integer estimatedDays) {}
