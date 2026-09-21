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
        String district
) {
}
