package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.InventoryItem;
import mexa.club.warehouseproject.entity.InventoryItemStatus;
import mexa.club.warehouseproject.entity.InventorySession;
import mexa.club.warehouseproject.entity.InventoryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySessionResponse {
    private UUID id;
    private UUID warehouseId;
    private InventoryStatus status;
    private UUID startedBy;
    private UUID completedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String note;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private UUID id;
        private UUID productId;
        private String productName;
        private BigDecimal systemQuantity;
        private BigDecimal countedQuantity;
        private BigDecimal difference;
        private InventoryItemStatus status;
    }

    public static InventorySessionResponse fromEntity(InventorySession session) {
        return InventorySessionResponse.builder()
                .id(session.getId())
                .warehouseId(session.getWarehouseId())
                .status(session.getStatus())
                .startedBy(session.getStartedBy())
                .completedBy(session.getCompletedBy())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .note(session.getNote())
                .items(session.getItems() == null ? List.of() : session.getItems().stream().map(InventorySessionResponse::item).toList())
                .build();
    }

    private static Item item(InventoryItem i) {
        return Item.builder()
                .id(i.getId())
                .productId(i.getProductId())
                .systemQuantity(i.getSystemQuantity())
                .countedQuantity(i.getCountedQuantity())
                .difference(i.getDifference())
                .status(i.getStatus())
                .build();
    }
}
