package mexa.club.orderservice.repository;

import mexa.club.orderservice.entity.Order;
import mexa.club.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, OrderRepositoryCustom {
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    @Query("select count(o) from Order o where o.createdAt between :from and :to")
    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select coalesce(sum(o.totalAmount),0) from Order o where o.closedAt between :from and :to and o.status <> mexa.club.orderservice.entity.OrderStatus.CANCELLED")
    BigDecimal revenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(o.discountAmount), 0) from Order o where o.closedAt between :from and :to and o.status <> mexa.club.orderservice.entity.OrderStatus.CANCELLED")
    BigDecimal discountGivenBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> countByStatus();

    // Status breakdown with date range
    @Query("select o.status, count(o) from Order o where o.createdAt between :from and :to group by o.status")
    List<Object[]> countByStatusBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Paid count in range (PAID paymentStatus)
    @Query("select count(o) from Order o where o.createdAt between :from and :to and o.paymentStatus = mexa.club.orderservice.entity.PaymentStatus.PAID")
    long countPaidBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Cancelled count in range
    @Query("select count(o) from Order o where o.createdAt between :from and :to and o.status = mexa.club.orderservice.entity.OrderStatus.CANCELLED")
    long countCancelledBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Top products by quantity sold in range (yopilgan buyurtmalar bo'yicha — daromad kabi)
    @Query("""
            select i.productId, i.productName, sum(i.quantity), sum(i.subtotal)
            from OrderItem i
            join i.order o
            where o.closedAt between :from and :to
              and o.status <> mexa.club.orderservice.entity.OrderStatus.CANCELLED
            group by i.productId, i.productName
            order by sum(i.quantity) desc
            """)
    List<Object[]> topProductsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("select count(o) from Order o where o.status = mexa.club.orderservice.entity.OrderStatus.PENDING")
    long countPending();
}
