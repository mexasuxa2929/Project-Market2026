package mexa.club.shopservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @Size(max = 128) String label,
        @NotBlank @Size(max = 512) String name,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @Size(max = 512) String line2,
        @Size(max = 64) String phone,
        boolean defaultAddress
) {
}
