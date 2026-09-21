package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.StockLot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLotResponse {

    private UUID id;
    private UUID warehouseId;
    private UUID productId;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private UUID purchaseId;
    private UUID purchaseItemId;
    private LocalDateTime receivedDate;

    public static StockLotResponse fromEntity(StockLot l, String productName) {
        BigDecimal qty = l.getQuantity() != null ? l.getQuantity() : BigDecimal.ZERO;
        BigDecimal unitCost = l.getUnitCost() != null ? l.getUnitCost() : BigDecimal.ZERO;
        return StockLotResponse.builder()
                .id(l.getId())
                .warehouseId(l.getWarehouseId())
                .productId(l.getProductId())
                .productName(productName)
                .quantity(qty)
                .unitCost(unitCost)
                .totalCost(qty.multiply(unitCost))
                .purchaseId(l.getPurchaseId())
                .purchaseItemId(l.getPurchaseItemId())
                .receivedDate(l.getReceivedDate())
                .build();
    }
}
