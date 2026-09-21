package mexa.club.warehouseproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID warehouseId;

    @Column(nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private MovementStatus status;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_cost", precision = 19, scale = 6)
    private BigDecimal unitCost;

    @Column(name = "total_cost", precision = 19, scale = 6)
    private BigDecimal totalCost;

    private UUID referenceId;

    @Column(length = 32)
    private String referenceType;

    @Column(length = 500)
    private String reason;

    private UUID actorUserId;

    @Column(length = 128)
    private String actorUsername;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
