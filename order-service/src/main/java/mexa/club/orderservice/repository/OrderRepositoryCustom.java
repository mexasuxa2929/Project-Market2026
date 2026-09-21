package mexa.club.orderservice.repository;

import mexa.club.orderservice.entity.Order;
import mexa.club.orderservice.entity.OrderStatus;
import mexa.club.orderservice.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderRepositoryCustom {
    Page<Order> searchOrders(
            OrderStatus status,
            PaymentStatus paymentStatus,
            UUID userId,
            String orderNumber,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            UUID warehouseId,
            Pageable pageable);
}
