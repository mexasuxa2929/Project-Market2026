package mexa.club.deliveryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * H2: order-service'ga DELIVERED/FAILED xabari yuborilmagan bo'lsa shu jadvalda
 * qoladi; scheduled retry uni qayta yuboradi (xabar yo'qolib ketmaydi).
 */
@Entity
@Table(name = "order_notify_outbox")
@Getter
@Setter
@NoArgsConstructor
public class OrderNotifyOutbox {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** DELIVERED | FAILED */
    @Column(nullable = false)
    private String kind;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
