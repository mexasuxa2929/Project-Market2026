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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_zone_audit")
@Getter
@Setter
public class DeliveryZoneAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "zone_id")
    private UUID zoneId;
    @Column(nullable = false, length = 16)
    private String action;
    @Column(name = "old_fee", precision = 19, scale = 2)
    private BigDecimal oldFee;
    @Column(name = "new_fee", precision = 19, scale = 2)
    private BigDecimal newFee;
    @Column(name = "old_per_km_fee", precision = 19, scale = 2)
    private BigDecimal oldPerKmFee;
    @Column(name = "new_per_km_fee", precision = 19, scale = 2)
    private BigDecimal newPerKmFee;
    @Column(name = "old_per_kg_fee", precision = 19, scale = 2)
    private BigDecimal oldPerKgFee;
    @Column(name = "new_per_kg_fee", precision = 19, scale = 2)
    private BigDecimal newPerKgFee;
    @Column(name = "old_free_above", precision = 19, scale = 2)
    private BigDecimal oldFreeAbove;
    @Column(name = "new_free_above", precision = 19, scale = 2)
    private BigDecimal newFreeAbove;
    @Column(name = "changed_by", length = 150)
    private String changedBy;
    /** D-H7: buzuvchi amal sababi (delete/update reason). */
    @Column(name = "reason", length = 255)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
