package mexa.club.deliveryservice.dto;

import jakarta.validation.constraints.NotNull;
import mexa.club.deliveryservice.entity.DeliveryStatus;

public record UpdateDeliveryStatusRequest(
        @NotNull DeliveryStatus status,
        String location
) {}
