package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.MovementStatus;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.entity.StockMovement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {
    private UUID id;
    private MovementType movementType;
    private MovementStatus status;
    private UUID productId;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String referenceType;
    private UUID referenceId;
    private String reason;
    private String actorUsername;
    private LocalDateTime createdAt;

    public static StockMovementResponse fromEntity(StockMovement m, String productName) {
        return StockMovementResponse.builder()
                .id(m.getId())
                .movementType(m.getMovementType())
                .status(m.getStatus() != null ? m.getStatus() : MovementStatus.CONFIRMED)
                .productId(m.getProductId())
                .productName(productName)
                .quantity(m.getQuantity())
                .unitCost(m.getUnitCost())
                .totalCost(m.getTotalCost())
                .referenceType(m.getReferenceType())
                .referenceId(m.getReferenceId())
                .reason(m.getReason())
                .actorUsername(m.getActorUsername())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
