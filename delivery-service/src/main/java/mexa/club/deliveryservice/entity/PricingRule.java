package mexa.club.deliveryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Asosiy delivery pricing qoidasi: fee = base + km*perKm + kg*perKg (min/max klamp). */
@Entity
@Table(name = "delivery_pricing_rules", indexes = {
        @Index(name = "idx_pricing_rules_active", columnList = "active")
})
@Getter
@Setter
public class PricingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "zone_id")
    private UUID zoneId;

    /** NULL = barcha delivery turlari uchun. */
    @Column(name = "delivery_type", length = 32)
    private String deliveryType;

    @Column(name = "base_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseFee = BigDecimal.ZERO;

    @Column(name = "price_per_km", nullable = false, precision = 19, scale = 2)
    private BigDecimal pricePerKm = BigDecimal.ZERO;

    @Column(name = "price_per_kg", nullable = false, precision = 19, scale = 2)
    private BigDecimal pricePerKg = BigDecimal.ZERO;

    @Column(name = "min_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal minFee = BigDecimal.ZERO;

    @Column(name = "max_fee", precision = 19, scale = 2)
    private BigDecimal maxFee;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "change_reason", length = 255)
    private String changeReason;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
