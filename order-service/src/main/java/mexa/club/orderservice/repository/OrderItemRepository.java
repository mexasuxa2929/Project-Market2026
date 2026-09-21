package mexa.club.orderservice.repository;

import mexa.club.orderservice.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("""
            select i.productId, sum(i.quantity), max(i.productName), coalesce(sum(i.subtotal), 0)
            from OrderItem i join i.order o
            where o.createdAt between :from and :to
            group by i.productId
            order by sum(i.quantity) desc
            """)
    Page<Object[]> topProductsByQuantity(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    /** Mahsulotga bog'liq (yakunlanmagan yoki istalgan) orderlar soni. */
    long countByProductId(UUID productId);
}
