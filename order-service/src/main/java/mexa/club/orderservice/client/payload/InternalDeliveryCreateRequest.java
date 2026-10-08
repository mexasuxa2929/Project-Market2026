package mexa.club.orderservice.client.payload;

import java.util.UUID;

/** delivery-service /internal/deliveries/create so'rovi. */
public record InternalDeliveryCreateRequest(
        UUID orderId,
        UUID warehouseId,
        String address,
        String recipientName,
        String recipientPhone,
        String region,
        String district,
        Double weightKg,
        java.math.BigDecimal orderSubtotal,
        /** D-H1: ombor ↔ manzil orasidagi masofa (km) — narx engine uchun. */
        Double distanceKm
) {
    public InternalDeliveryCreateRequest(
            UUID orderId, UUID warehouseId, String address,
            String recipientName, String recipientPhone,
            String region, String district) {
        this(orderId, warehouseId, address, recipientName, recipientPhone, region, district, null, null, null);
    }
}
