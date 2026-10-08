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
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

/** Qo'shimcha fee (peak hour, og'irlik, express, night, holiday...). */
@Entity
@Table(name = "delivery_surcharges")
@Getter
@Setter
public class DeliverySurcharge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String name;

    /** PEAK_HOUR | NIGHT | HOLIDAY | HEAVY_ORDER | EXPRESS | HIGH_DEMAND | REMOTE_AREA | WEATHER */
    @Column(name = "condition_type", nullable = false, length = 24)
    private String conditionType;

    /** GT | GTE | LT | LTE | EQ */
    @Column(nullable = false, length = 8)
    private String operator = "GTE";

    @Column(name = "condition_value", precision = 19, scale = 2)
    private BigDecimal conditionValue;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    /** FIXED | PERCENT */
    @Column(name = "fee_type", nullable = false, length = 8)
    private String feeType = "FIXED";

    @Column(name = "time_from")
    private LocalTime timeFrom;

    @Column(name = "time_to")
    private LocalTime timeTo;

    @Column(name = "weight_from", precision = 10, scale = 2)
    private BigDecimal weightFrom;

    @Column(name = "weight_to", precision = 10, scale = 2)
    private BigDecimal weightTo;

    @Column(name = "delivery_type", length = 32)
    private String deliveryType;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false)
    private Boolean active = true;

    /** D-H2: surcharge faqat shu davrda faol (NULL = cheklov yo'q). */
    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
