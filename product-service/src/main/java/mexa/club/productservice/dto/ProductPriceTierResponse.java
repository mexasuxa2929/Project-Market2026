package mexa.club.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mexa.club.productservice.entity.ProductPriceTier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceTierResponse {
    private UUID id;
    private UUID productId;
    private int tierIndex;          // tartib raqami (1-dan boshlanadi)
    private int minQty;
    private Integer maxQty;         // null = cheksiz
    private BigDecimal price;
    private String currency;
    private String priceType;
    private String rangeLabel;      // "1 – 9 units" yoki "100+ units"
    private BigDecimal exampleTotal; // minQty * price
    private Double discountPercent; // birinchi tier ga nisbatan chegirma foizi (null = birinchi tier)

    /** Indeksni va base narxini bilmay turib yaratish (to'liq ma'lumot uchun fromEntityList ishlating) */
    public static ProductPriceTierResponse fromEntity(ProductPriceTier e) {
        return fromEntity(e, 1, null);
    }

    public static ProductPriceTierResponse fromEntity(ProductPriceTier e, int tierIndex, BigDecimal basePrice) {
        String range = e.getMaxQty() == null
                ? e.getMinQty() + "+ units"
                : e.getMinQty() + " – " + e.getMaxQty() + " units";

        int exQty = e.getMinQty() > 0 ? e.getMinQty() : 1;
        BigDecimal example = e.getPrice().multiply(BigDecimal.valueOf(exQty));

        Double discount = null;
        if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0
                && e.getPrice().compareTo(basePrice) < 0) {
            discount = basePrice.subtract(e.getPrice())
                    .divide(basePrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new ProductPriceTierResponse(
                e.getId(),
                e.getProductId(),
                tierIndex,
                e.getMinQty(),
                e.getMaxQty(),
                e.getPrice(),
                e.getCurrency(),
                e.getPriceType(),
                range,
                example,
                discount
        );
    }

    /** Ro'yxat uchun — indeks va chegirma to'g'ri hisoblanadi */
    public static List<ProductPriceTierResponse> fromEntityList(List<ProductPriceTier> entities) {
        BigDecimal basePrice = entities.isEmpty() ? null : entities.get(0).getPrice();
        var result = new java.util.ArrayList<ProductPriceTierResponse>();
        for (int i = 0; i < entities.size(); i++) {
            result.add(fromEntity(entities.get(i), i + 1, i == 0 ? null : basePrice));
        }
        return result;
    }
}
