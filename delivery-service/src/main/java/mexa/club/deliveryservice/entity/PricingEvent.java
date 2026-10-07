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

/** Har bir pricing hisoblashidan analytics uchun yoziladigan event. */
@Entity
@Table(name = "delivery_pricing_events", indexes = {
        @Index(name = "idx_pricing_events_created", columnList = "created_at"),
        @Index(name = "idx_pricing_events_order", columnList = "order_id")
})
@Getter
@Setter
public class PricingEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "cart_total", precision = 19, scale = 2)
    private BigDecimal cartTotal;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "zone_id")
    private UUID zoneId;

    @Column(name = "delivery_type", length = 32)
    private String deliveryType;

    @Column(name = "base_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseFee = BigDecimal.ZERO;

    @Column(name = "distance_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal distanceFee = BigDecimal.ZERO;

    @Column(name = "weight_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal weightFee = BigDecimal.ZERO;

    @Column(name = "surcharge_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal surchargeTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "final_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalFee = BigDecimal.ZERO;

    @Column(name = "estimated_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal margin = BigDecimal.ZERO;

    @Column(name = "free_delivery", nullable = false)
    private Boolean freeDelivery = false;

    @Column(name = "threshold_amount", precision = 19, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
