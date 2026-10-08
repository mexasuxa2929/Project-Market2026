package mexa.club.paymentservice.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderNotifyOutboxRepository extends JpaRepository<PaymentOrderNotifyOutbox, UUID> {

    Optional<PaymentOrderNotifyOutbox> findByOrderServiceIdAndActionAndSentAtIsNull(UUID orderServiceId, String action);

    @Query("select o from PaymentOrderNotifyOutbox o where o.sentAt is null and o.nextAttemptAt <= :now "
            + "order by o.nextAttemptAt asc")
    List<PaymentOrderNotifyOutbox> findDue(@Param("now") LocalDateTime now);
}
