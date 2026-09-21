package mexa.club.shopservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartQuantityRequest(
        @NotNull @Min(1) @Max(9999) Integer quantity
) {
}
