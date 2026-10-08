package mexa.club.paymentservice.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** H4: order-service'ga yuborilmagan payment xabarlari uchun outbox. */
@Entity
@Table(name = "payment_order_notify_outbox")
@Getter
@Setter
@NoArgsConstructor
public class PaymentOrderNotifyOutbox {

    @Id
    private UUID id;

    @Column(name = "order_service_id", nullable = false)
    private UUID orderServiceId;

    /** payment-confirmed | payment-failed */
    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
