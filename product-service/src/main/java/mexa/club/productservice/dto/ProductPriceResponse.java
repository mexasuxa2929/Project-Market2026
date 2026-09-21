package mexa.club.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.productservice.entity.ProductPrice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceResponse {

    private UUID id;
    private UUID productId;

    /** Asl (chegirmasiz) sotuv narxi. */
    private BigDecimal salePrice;
    private LocalDateTime effectiveDate;
    private LocalDateTime endDate;

    private Integer discountPercent;
    private LocalDateTime discountStartDate;
    private LocalDateTime discountEndDate;

    /** Chegirma hozir faolmi (sanalar oralig'ida). */
    private boolean discountActive;

    /** Joriy haqiqiy narx — chegirma faol bo'lsa hisoblangan, aks holda salePrice. */
    private BigDecimal currentPrice;

    public static ProductPriceResponse fromEntity(ProductPrice e) {
        return fromEntity(e, LocalDateTime.now());
    }

    public static ProductPriceResponse fromEntity(ProductPrice e, java.time.LocalDateTime now) {
        boolean active = isDiscountActive(e, now);
        BigDecimal current = active && e.getSalePrice() != null && e.getDiscountPercent() != null
                ? e.getSalePrice().multiply(BigDecimal.valueOf(100 - e.getDiscountPercent()))
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                : e.getSalePrice();
        return ProductPriceResponse.builder()
                .id(e.getId())
                .productId(e.getProductId())
                .salePrice(e.getSalePrice())
                .effectiveDate(e.getEffectiveDate())
                .endDate(e.getEndDate())
                .discountPercent(e.getDiscountPercent())
                .discountStartDate(e.getDiscountStartDate())
                .discountEndDate(e.getDiscountEndDate())
                .discountActive(active)
                .currentPrice(current)
                .build();
    }

    /** Chegirma shu vaqtda faolmi: foiz > 0 va now [start, end] oralig'ida (null chegara = cheksiz). */
    public static boolean isDiscountActive(ProductPrice e, LocalDateTime now) {
        if (e == null || e.getDiscountPercent() == null || e.getDiscountPercent() <= 0) {
            return false;
        }
        if (e.getDiscountStartDate() != null && now.isBefore(e.getDiscountStartDate())) {
            return false;
        }
        return e.getDiscountEndDate() == null || !now.isAfter(e.getDiscountEndDate());
    }
}

