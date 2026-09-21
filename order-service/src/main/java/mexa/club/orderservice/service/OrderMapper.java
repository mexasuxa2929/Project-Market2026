package mexa.club.orderservice.service;

import mexa.club.orderservice.dto.OrderItemResponse;
import mexa.club.orderservice.dto.OrderResponse;
import mexa.club.orderservice.entity.Order;
import mexa.club.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(this::toItemResponse).toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getStatus(),
                order.getDeliveryAddressId(),
                order.getDeliveryAddress(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getEstimatedDeliveryDays(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getNote(),
                order.getCancelReason(),
                order.getPaymentStatus(),
                order.getPaymentId(),
                order.getPaymentMethod(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getClosedAt(),
                items,
                order.getDiscountCode(),
                order.getDiscountAmount(),
                order.getAdminNote()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getImageUrl(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
