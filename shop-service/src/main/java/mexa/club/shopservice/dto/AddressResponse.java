package mexa.club.shopservice.dto;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        String label,
        String name,
        Double latitude,
        Double longitude,
        String line2,
        String phone,
        boolean defaultAddress,
        Instant createdAt,
        Instant updatedAt
) {
}
