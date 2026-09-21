package mexa.club.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_status_history")
@Getter
@Setter
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID orderId;
    @Column(length = 32)
    private String fromStatus;
    @Column(nullable = false, length = 32)
    private String toStatus;
    @Column(nullable = false)
    private UUID changedBy;
    @Column(length = 500)
    private String reason;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
