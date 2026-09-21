package mexa.club.orderservice.client.payload;

import java.util.UUID;

/** delivery-service DeliveryResponse dan kerakli maydonlar. */
public record DeliveryCreatedPayload(
        UUID id,
        String trackingCode,
        String status
) {
}
