package mexa.club.productservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Barcha tier larni bir yo'la almashtirish uchun (PUT /price-tiers/bulk).
 * Mavjud tier lar o'chiriladi va yangilari saqlanadi.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductPriceTierBulkRequest {

    @NotNull
    @Valid
    private List<ProductPriceTierRequest> tiers;
}
