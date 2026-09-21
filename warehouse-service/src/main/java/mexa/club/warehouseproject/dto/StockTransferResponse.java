package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.StockTransfer;
import mexa.club.warehouseproject.entity.StockTransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferResponse {

    private UUID id;
    private UUID fromWarehouseId;
    private String fromWarehouseName;
    private UUID toWarehouseId;
    private String toWarehouseName;
    private UUID productId;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private StockTransferStatus status;
    private String note;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private UUID approvedBy;
    private LocalDateTime approvedAt;

    public static StockTransferResponse fromEntity(
            StockTransfer t,
            String fromWarehouseName,
            String toWarehouseName,
            String productName
    ) {
        return StockTransferResponse.builder()
                .id(t.getId())
                .fromWarehouseId(t.getFromWarehouseId())
                .fromWarehouseName(fromWarehouseName)
                .toWarehouseId(t.getToWarehouseId())
                .toWarehouseName(toWarehouseName)
                .productId(t.getProductId())
                .productName(productName)
                .quantity(t.getQuantity())
                .unitCost(t.getUnitCost())
                .status(t.getStatus())
                .note(t.getNote())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .approvedBy(t.getApprovedBy())
                .approvedAt(t.getApprovedAt())
                .build();
    }
}
