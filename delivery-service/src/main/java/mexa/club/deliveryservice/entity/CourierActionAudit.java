package mexa.club.deliveryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "courier_action_audit")
@Getter
@Setter
public class CourierActionAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "courier_id")
    private UUID courierId;
    @Column(name = "courier_name", length = 150)
    private String courierName;
    @Column(nullable = false, length = 32)
    private String action;
    @Column(name = "delivery_id")
    private UUID deliveryId;
    @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "warehouse_id")
    private UUID warehouseId;
    @Column(name = "actor_id")
    private UUID actorId;
    @Column(name = "actor_username", length = 150)
    private String actorUsername;
    @Column(name = "actor_role", length = 32)
    private String actorRole;
    @Column(length = 500)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
