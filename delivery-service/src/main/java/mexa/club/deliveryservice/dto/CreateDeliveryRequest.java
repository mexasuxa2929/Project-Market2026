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
        String district,
        /** Avtomatik narx uchun ixtiyoriy qismlar: bo'lmasa 0 hisoblanadi. */
        Double distanceKm,
        Double weightKg,
        java.math.BigDecimal orderSubtotal,
        /** STANDARD | EXPRESS | SCHEDULED (NULL = STANDARD). */
        String deliveryType
) {
    public CreateDeliveryRequest(
            UUID orderId, UUID warehouseId, String address,
            String recipientName, String recipientPhone,
            String region, String district) {
        this(orderId, warehouseId, address, recipientName, recipientPhone, region, district, null, null, null, null);
    }
}
