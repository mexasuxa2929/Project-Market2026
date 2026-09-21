package mexa.club.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateDeliveryRequest(
        @NotNull UUID orderId,
        @NotNull UUID warehouseId,
        @NotBlank String address,
        @NotBlank String recipientName,
        @Size(max = 64) String recipientPhone,
        String region,
        String district
) {}
