package mexa.club.shopservice.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponsePayload(
        UUID id,
        String orderNumber,
        UUID userId,
        OrderStatusPayload status,
        UUID deliveryAddressId,
        String deliveryAddress,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        Integer estimatedDeliveryDays,
        BigDecimal totalAmount,
        String currency,
        String note,
        String cancelReason,
        PaymentStatusPayload paymentStatus,
        UUID paymentId,
        String paymentMethod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemPayload> items
) {
}
