package mexa.club.paymentservice.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payme_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymeTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paycom_tx_id", nullable = false, unique = true, length = 128)
    private String paycomTxId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long amount;

    /** 1 created, 2 completed, -1 canceled before perform, -2 canceled after perform */
    @Column(nullable = false)
    private Short state;

    private Integer reason;

    @Column(name = "create_time", nullable = false)
    private Long createTime;

    @Column(name = "perform_time")
    private Long performTime;

    @Column(name = "cancel_time")
    private Long cancelTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
