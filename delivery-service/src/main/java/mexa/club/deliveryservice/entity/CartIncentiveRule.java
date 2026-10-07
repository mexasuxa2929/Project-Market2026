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

/** Cart incentive: kattaroq buyurtmaga undash qoidasi (threshold + reward + xabar). */
@Entity
@Table(name = "cart_incentive_rules")
@Getter
@Setter
public class CartIncentiveRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "threshold_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal thresholdAmount;

    /** FREE | AMOUNT | PERCENT */
    @Column(name = "reward_type", nullable = false, length = 16)
    private String rewardType = "FREE";

    @Column(name = "reward_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal rewardValue = BigDecimal.ZERO;

    @Column(name = "message_template", nullable = false, length = 500)
    private String messageTemplate = "Yana {remaining} so'mlik mahsulot qo'shing va bepul yetkazib berishdan foydalaning.";

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
