package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertResponse {

    private UUID warehouseId;
    private UUID productId;
    private String productName;
    private int minStock;
    /** Ombor darajasidagi override. null bo'lsa mahsulot default'i ishlatilgan. */
    private Integer minStockOverride;
    private BigDecimal quantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
}
