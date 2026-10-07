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

/** Delivery turi: STANDARD / EXPRESS / SCHEDULED. */
@Entity
@Table(name = "delivery_types")
@Getter
@Setter
public class DeliveryType {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String name;

    @Column(name = "base_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseFee = BigDecimal.ZERO;

    @Column(name = "additional_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal additionalFee = BigDecimal.ZERO;

    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes = 60;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
