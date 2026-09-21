package mexa.club.orderservice.dto;

import mexa.club.orderservice.entity.OrderStatus;
import mexa.club.orderservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID userId,
        OrderStatus status,
        UUID deliveryAddressId,
        String deliveryAddress,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        Integer estimatedDeliveryDays,
        BigDecimal totalAmount,
        String currency,
        String note,
        String cancelReason,
        PaymentStatus paymentStatus,
        UUID paymentId,
        String paymentMethod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt,
        List<OrderItemResponse> items,
        String discountCode,
        BigDecimal discountAmount,
        String adminNote
) {}
