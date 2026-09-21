package mexa.club.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSummaryResponse {
    private UUID productId;
    private String productName;
    private BigDecimal totalQuantity;
    @Builder.Default
    private List<WarehouseQuantity> warehouses = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseQuantity {
        private UUID warehouseId;
        private String warehouseName;
        private BigDecimal quantity;
        private BigDecimal reservedQuantity;
        private BigDecimal availableQuantity;
    }
}
