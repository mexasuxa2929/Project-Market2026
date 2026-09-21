package mexa.club.deliveryservice.client.payload;

import java.util.Map;
import java.util.UUID;

/**
 * notification-service {@code SendNotificationRequest} bilan mos JSON maydonlari.
 * type/channel — enum nomlari (masalan DELIVERY_ASSIGNED, SMS).
 */
public record InternalNotificationPayload(
        UUID userId,
        String type,
        String channel,
        String recipientEmail,
        String recipientPhone,
        Map<String, String> variables
) {
}
