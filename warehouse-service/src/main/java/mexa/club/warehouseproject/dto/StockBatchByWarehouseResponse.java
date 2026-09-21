package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchByWarehouseResponse {
    private UUID productId;
    private List<WarehouseBreakdown> warehouses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseBreakdown {
        private UUID warehouseId;
        private String warehouseName;
        private BigDecimal quantity;
        private BigDecimal reservedQuantity;
        private BigDecimal availableQuantity;
    }
}
