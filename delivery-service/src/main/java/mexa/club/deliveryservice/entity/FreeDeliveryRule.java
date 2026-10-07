package mexa.club.deliveryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Free delivery threshold qoidasi: order >= minOrder → delivery = deliveryFee. */
@Entity
@Table(name = "free_delivery_rules")
@Getter
@Setter
public class FreeDeliveryRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "min_order", nullable = false, precision = 19, scale = 2)
    private BigDecimal minOrder;

    @Column(name = "delivery_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "zone_id")
    private UUID zoneId;

    @Column(name = "delivery_type", length = 32)
    private String deliveryType;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
