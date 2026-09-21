package mexa.club.shopservice.dto;

import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @Size(max = 128) String label,
        @Size(max = 512) String name,
        Double latitude,
        Double longitude,
        @Size(max = 512) String line2,
        @Size(max = 64) String phone,
        Boolean defaultAddress
) {
}
