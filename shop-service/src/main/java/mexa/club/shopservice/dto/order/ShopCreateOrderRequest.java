package mexa.club.shopservice.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ShopCreateOrderRequest(
        UUID deliveryAddressId,
        String deliveryAddress,
        String note,
        @NotEmpty List<@Valid ShopCreateOrderItemRequest> items,
        String discountCode,
        String paymentMethod
) {
}
