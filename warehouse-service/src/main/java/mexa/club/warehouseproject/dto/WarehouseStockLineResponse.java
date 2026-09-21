package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.WarehouseStock;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockLineResponse {

    private UUID id;
    private UUID warehouseId;
    private UUID productId;
    /** Resolved from product-service at runtime. */
    private String productName;
    private BigDecimal quantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
    /** Ushbu omborda qo'llaniladigan minimal stock (override o'rnatilgan bo'lsa shu, aks holda mahsulot default'i). */
    private int minStock;
    /** Ombor darajasidagi override. null bo'lsa mahsulotning default minStock'i ishlatiladi. */
    private Integer minStockOverride;
    /** Band qilingan hajm (m³) = quantity × (length×width×height / 1_000_000). */
    private BigDecimal volumeM3;

    public static WarehouseStockLineResponse fromEntity(WarehouseStock s) {
        return WarehouseStockLineResponse.builder()
                .id(s.getId())
                .warehouseId(s.getWarehouse() != null ? s.getWarehouse().getId() : null)
                .productId(s.getProductId())
                .productName(null)
                .quantity(s.getQuantity())
                .reservedQuantity(s.getReservedQuantity() != null ? s.getReservedQuantity() : BigDecimal.ZERO)
                .availableQuantity((s.getQuantity() != null ? s.getQuantity() : BigDecimal.ZERO)
                        .subtract(s.getReservedQuantity() != null ? s.getReservedQuantity() : BigDecimal.ZERO))
                .minStock(0)
                .minStockOverride(s.getMinStockOverride())
                .volumeM3(BigDecimal.ZERO)
                .build();
    }
}
