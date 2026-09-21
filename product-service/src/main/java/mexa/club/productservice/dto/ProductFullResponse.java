package mexa.club.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.productservice.entity.Product;
import mexa.club.productservice.entity.ProductPrice;
import mexa.club.productservice.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFullResponse {

    private UUID id;
    private String name;
    private String barcode;
    private String color;
    private String colorCode;
    private UUID groupId;
    private UUID categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private UUID manufacturerId;
    private String manufacturerName;
    private String unit;
    private int leadTimeDays;
    private int minStock;
    private double weight;
    private double length;
    private double width;
    private double height;
    private String packageType;
    private boolean fragile;
    private boolean active;
    private boolean featured;
    private boolean digital;
    private ProductStatus status;
    private String description;
    private String shortDescription;
    private String slug;
    private String material;
    private String countryOfOrigin;
    private int warrantyMonths;

    @Builder.Default
    private Map<String, String> nameTranslations = Map.of();
    @Builder.Default
    private Map<String, String> descriptionTranslations = Map.of();
    @Builder.Default
    private Map<String, String> shortDescriptionTranslations = Map.of();
    @Builder.Default
    private Map<String, String> materialTranslations = Map.of();
    @Builder.Default
    private Map<String, String> countryOfOriginTranslations = Map.of();
    @Builder.Default
    private Map<String, String> manufacturerNameTranslations = Map.of();

    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<String> imageUrls = List.of();

    /** Guruhlangan rasm variantlari — har bir rasm original/thumb/medium bilan */
    @Builder.Default
    private List<ProductImageVariants> images = List.of();

    @Builder.Default
    private Set<String> tags = Set.of();

    /** Base price — joriy haqiqiy narx (chegirma faol bo'lsa hisoblangan, aks holda salePrice). */
    private BigDecimal basePrice;

    /** Asl (chegirmasiz) sotuv narxi — so'nggi ProductPrice'dan. */
    private BigDecimal salePrice;

    /** Aktiv chegirma foizi (muddat o'tgan bo'lsa 0). */
    @Builder.Default
    private Integer discountPercent = 0;

    /** Chegirma hozir faolmi (sanalar oralig'ida). */
    @Builder.Default
    private boolean discountActive = false;

    private LocalDateTime discountStartDate;
    private LocalDateTime discountEndDate;

    /** Shu mahsulot uchun barcha omborlardagi umumiy qoldiq. */
    private Integer totalStock;

    private boolean inStock;

    /** Bir guruhdagi (group_id) boshqa rangdagi nusxalar — har biri mustaqil Product. */
    @Builder.Default
    private List<ProductColorSiblingResponse> siblingColors = List.of();

    /** Liniyalar (miqdorga bog'liq narxlar) soni. */
    private Integer tierCount = 0;

    public static ProductFullResponse fromEntity(
            Product p,
            BigDecimal basePrice,
            ProductPrice latestPrice,
            int tierCount,
            Integer totalStock,
            List<String> imageUrls,
            List<ProductImageVariants> images,
            String categoryName,
            String brandName,
            String manufacturerName,
            Set<String> tags,
            List<ProductColorSiblingResponse> siblingColors
    ) {
        // Chegirma faol bo'lsa liniyalar ishlamaydi (chegirma 1-liniya narxiga qo'llanadi).
        boolean discountActive = ProductPriceResponse.isDiscountActive(latestPrice, LocalDateTime.now());
        return ProductFullResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .barcode(p.getBarcode())
                .color(p.getColor())
                .colorCode(p.getColorCode())
                .groupId(p.getGroupId())
                .categoryId(p.getCategoryId())
                .categoryName(categoryName)
                .brandId(p.getBrandId())
                .brandName(brandName)
                .manufacturerId(p.getManufacturerId())
                .manufacturerName(manufacturerName)
                .unit(p.getUnit())
                .leadTimeDays(p.getLeadTimeDays())
                .minStock(p.getMinStock())
                .weight(p.getWeight())
                .length(p.getLength())
                .width(p.getWidth())
                .height(p.getHeight())
                .packageType(p.getPackageType())
                .fragile(p.isFragile())
                .active(p.isActive())
                .featured(p.isFeatured())
                .digital(p.isDigital())
                .status(p.getStatus())
                .description(p.getDescription())
                .shortDescription(p.getShortDescription())
                .slug(p.getSlug())
                .material(p.getMaterial())
                .countryOfOrigin(p.getCountryOfOrigin())
                .warrantyMonths(p.getWarrantyMonths())
                .nameTranslations(p.getNameTranslations())
                .descriptionTranslations(p.getDescriptionTranslations())
                .shortDescriptionTranslations(p.getShortDescriptionTranslations())
                .materialTranslations(p.getMaterialTranslations())
                .countryOfOriginTranslations(p.getCountryOfOriginTranslations())
                .manufacturerNameTranslations(p.getManufacturerNameTranslations())
                .createdBy(p.getCreatedBy())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .basePrice(basePrice)
                .salePrice(latestPrice != null ? latestPrice.getSalePrice() : null)
                .discountPercent(discountActive && latestPrice.getDiscountPercent() != null ? latestPrice.getDiscountPercent() : 0)
                .discountActive(discountActive)
                .discountStartDate(latestPrice != null ? latestPrice.getDiscountStartDate() : null)
                .discountEndDate(latestPrice != null ? latestPrice.getDiscountEndDate() : null)
                .totalStock(totalStock != null ? totalStock : 0)
                .inStock(totalStock != null && totalStock > 0)
                .tierCount(tierCount)
                .imageUrls(imageUrls != null ? imageUrls : List.of())
                .images(images != null ? images : List.of())
                .tags(tags != null ? tags : Set.of())
                .siblingColors(siblingColors != null ? siblingColors : List.of())
                .build();
    }
}
