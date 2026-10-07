package mexa.club.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CourierRequest(
        UUID userId,
        @NotBlank String name,
        @NotBlank String phone,
        @NotBlank String region,
        boolean active,
        UUID warehouseId
) {
    public CourierRequest(UUID userId, String name, String phone, String region, boolean active) {
        this(userId, name, phone, region, active, null);
    }
}
