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
public class ProfitReportResponse {

    private UUID warehouseId;
    private int productCount;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal totalProfit;
    // Gross Profit = Revenue - COGS; Operating Profit = Gross - Operating Expenses
    private BigDecimal totalOperatingExpense;
    private BigDecimal operatingProfit;

    private List<ProductProfitLine> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductProfitLine {
        private UUID productId;
        private String productName;
        private BigDecimal soldQuantity;
        private BigDecimal returnedQuantity;
        private BigDecimal returnedCost;
        private BigDecimal salePrice;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
    }
}
