package mexa.club.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.productservice.entity.Product;
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
public class ProductResponse {

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
    private Integer deliveryDaysMin;
    private Integer deliveryDaysMax;
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
    private String sku;
    private String slug;
    private String metaDescription;
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
    /** Shu mahsulot bilan bir guruhdagi boshqa rangdagi nusxalar (qisqa ma'lumot). */
    @Builder.Default
    private List<ProductColorSiblingResponse> siblingColors = List.of();
    @Builder.Default
    private Set<String> tags = Set.of();

    /** Base price from the latest ProductPrice entry (shu mahsulot/rangning o'z narxi). */
    private BigDecimal basePrice;

    /** Shu mahsulot uchun barcha omborlardagi umumiy qoldiq. */
    private Integer totalStock;

    private Boolean inStock;

    /** O'rtacha reyting (shop-service reviewlaridan). */
    private Double avgRating;

    /** Reviewlar soni. */
    private Long reviewCount;

    /** Flash-chegirma foizi (0..100). Hozircha deterministik placeholder — real promo moduli kelganda almashtiriladi. */
    @Builder.Default
    private Integer discountPercent = 0;

    public static ProductResponse fromEntity(
            Product p,
            List<String> imageUrls,
            String categoryName,
            String brandName,
            String manufacturerName,
            List<ProductColorSiblingResponse> siblingColors,
            Set<String> tags
    ) {
        return fromEntity(p, imageUrls, categoryName, brandName, manufacturerName, siblingColors, tags, null, null, null, null, null);
    }

    public static ProductResponse fromEntity(
            Product p,
            List<String> imageUrls,
            String categoryName,
            String brandName,
            String manufacturerName,
            List<ProductColorSiblingResponse> siblingColors,
            Set<String> tags,
            BigDecimal basePrice,
            Integer totalStock,
            Boolean inStock,
            Double avgRating,
            Long reviewCount
    ) {
        return ProductResponse.builder()
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
                .deliveryDaysMin(p.getDeliveryDaysMin())
                .deliveryDaysMax(p.getDeliveryDaysMax())
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
                .sku(p.getSku())
                .slug(p.getSlug())
                .metaDescription(p.getMetaDescription())
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
                .imageUrls(imageUrls != null ? imageUrls : List.of())
                .siblingColors(siblingColors != null ? siblingColors : List.of())
                .tags(tags != null ? tags : Set.of())
                .basePrice(basePrice)
                .totalStock(totalStock)
                .inStock(inStock)
                .avgRating(avgRating)
                .reviewCount(reviewCount)
                .build();
    }
}

