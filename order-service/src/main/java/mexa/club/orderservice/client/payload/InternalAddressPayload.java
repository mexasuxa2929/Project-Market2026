package mexa.club.orderservice.client.payload;

import java.util.UUID;

/** shop-service /api/shops/internal/addresses/{id} javobi (nuqta modeli). */
public record InternalAddressPayload(
        UUID id,
        UUID userId,
        String label,
        String name,
        Double latitude,
        Double longitude,
        String line2,
        String phone,
        boolean defaultAddress
) {
}
