package mexa.club.deliveryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Global pricing sozlamalari — haqiqiy delivery cost (margin hisobi uchun). */
@Entity
@Table(name = "delivery_pricing_settings")
@Getter
@Setter
public class PricingSettings {
    /** Yagona qator (migration'da seed qilingan). */
    public static final UUID DEFAULT_ID = UUID.fromString("e1000000-0000-4000-8000-000000000001");

    @Id
    @Column(length = 36)
    private UUID id = DEFAULT_ID;

    @Column(name = "base_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseCost = BigDecimal.ZERO;

    @Column(name = "per_km_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal perKmCost = BigDecimal.ZERO;

    @Column(name = "per_kg_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal perKgCost = BigDecimal.ZERO;

    @Column(nullable = false, length = 16)
    private String currency = "UZS";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
