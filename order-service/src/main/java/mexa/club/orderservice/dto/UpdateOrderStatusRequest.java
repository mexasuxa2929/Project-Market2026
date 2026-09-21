package mexa.club.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import mexa.club.orderservice.entity.OrderStatus;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,
        String reason
) {}
