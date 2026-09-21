package mexa.club.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        UUID deliveryAddressId,
        String deliveryAddress,
        String note,
        @NotEmpty List<@Valid CreateOrderItemRequest> items,
        String discountCode,
        String paymentMethod
) {}
