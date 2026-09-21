package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.StockReturn;
import mexa.club.warehouseproject.entity.StockReturnItem;
import mexa.club.warehouseproject.entity.StockReturnStatus;
import mexa.club.warehouseproject.entity.ReturnCondition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReturnResponse {
    private UUID id;
    private UUID warehouseId;
    private UUID orderId;
    private UUID returnedBy;
    private StockReturnStatus status;
    private String reason;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private UUID approvedBy;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private UUID id;
        private UUID productId;
        private String productName;
        private BigDecimal quantity;
        private ReturnCondition condition;
    }

    public static StockReturnResponse fromEntity(StockReturn r) {
        return StockReturnResponse.builder()
                .id(r.getId())
                .warehouseId(r.getWarehouseId())
                .orderId(r.getOrderId())
                .returnedBy(r.getReturnedBy())
                .status(r.getStatus())
                .reason(r.getReason())
                .note(r.getNote())
                .createdAt(r.getCreatedAt())
                .approvedAt(r.getApprovedAt())
                .approvedBy(r.getApprovedBy())
                .items(r.getItems() == null ? List.of() : r.getItems().stream().map(StockReturnResponse::item).toList())
                .build();
    }

    private static Item item(StockReturnItem i) {
        return Item.builder()
                .id(i.getId())
                .productId(i.getProductId())
                .quantity(i.getQuantity())
                .condition(i.getCondition())
                .build();
    }
}
