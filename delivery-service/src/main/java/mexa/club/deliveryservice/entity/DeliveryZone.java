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
import java.util.UUID;

@Entity
@Table(name = "delivery_zone")
@Getter
@Setter
public class DeliveryZone {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 128)
    private String region;
    @Column(nullable = false, length = 128)
    private String district;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee;
    /** Har km uchun ustama (so'm). 0 = masofa hisobga olinmaydi. */
    @Column(name = "per_km_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal perKmFee = BigDecimal.ZERO;
    /** Har kg uchun ustama (so'm). 0 = og'irlik hisobga olinmaydi. */
    @Column(name = "per_kg_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal perKgFee = BigDecimal.ZERO;
    /** Shu summadan yuqori order uchun yetkazish bepul. NULL = chegara yo'q. */
    @Column(name = "free_above", precision = 19, scale = 2)
    private BigDecimal freeAbove;
    /** Narx klampi: eng kam to'lov (NULL = klamp yo'q). */
    @Column(name = "min_fee", precision = 19, scale = 2)
    private BigDecimal minFee;
    /** Narx klampi: eng ko'p to'lov (NULL = klamp yo'q). */
    @Column(name = "max_fee", precision = 19, scale = 2)
    private BigDecimal maxFee;
    /** Zonalar ustma ustida qaysi biri ustunlik qilishi (yuqori = birinchi). */
    @Column(nullable = false)
    private Integer priority = 100;
    @Column(nullable = false)
    private Integer estimatedDays = 1;
}
