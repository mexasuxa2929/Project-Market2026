package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.OrderNotifyOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderNotifyOutboxRepository extends JpaRepository<OrderNotifyOutbox, UUID> {

    Optional<OrderNotifyOutbox> findByOrderIdAndKindAndSentAtIsNull(UUID orderId, String kind);

    /** Retry uchun: yuborilmagan va navbatga qo'yilgan vaqti kelgan qatorlar. */
    @Query("select o from OrderNotifyOutbox o where o.sentAt is null and o.nextAttemptAt <= :now "
            + "order by o.nextAttemptAt asc")
    List<OrderNotifyOutbox> findDue(@Param("now") LocalDateTime now);
}
