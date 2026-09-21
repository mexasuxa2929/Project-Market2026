package mexa.club.shopservice.dto;

import java.util.UUID;

public record InternalAddressResponse(
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
