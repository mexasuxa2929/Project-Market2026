package mexa.club.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mexa.club.productservice.entity.ProductStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mahsulot yaratish/yangilash uchun — entity o‘rniga (mass assignment oldini olish).
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name must be at most 150 characters")
    private String name;

    @NotBlank(message = "barcode is required")
    @Size(max = 64, message = "barcode must be at most 64 characters")
    private String barcode;

    @Size(max = 60, message = "color must be at most 60 characters")
    private String color;

    @Size(max = 20, message = "colorCode must be at most 20 characters")
    private String colorCode;

    /** Bir xil mahsulotning boshqa rangdagi nusxasiga bog'lash uchun. Bo'sh qoldirilsa, mahsulot mustaqil hisoblanadi. */
    private UUID groupId;

    private UUID categoryId;
    private UUID brandId;
    private UUID manufacturerId;

    @Size(max = 100, message = "manufacturerName must be at most 100 characters")
    private String manufacturerName;

    @NotBlank(message = "unit is required")
    @Size(max = 32, message = "unit must be at most 32 characters")
    private String unit;

    @Min(value = 0, message = "leadTimeDays must be >= 0")
    private int leadTimeDays;

    @Min(value = 0, message = "deliveryDaysMin must be >= 0")
    private Integer deliveryDaysMin;

    @Min(value = 0, message = "deliveryDaysMax must be >= 0")
    private Integer deliveryDaysMax;

    @Min(value = 0, message = "minStock must be >= 0")
    private int minStock;

    @DecimalMin(value = "0.0", inclusive = true, message = "weight must be >= 0.0")
    private double weight;

    @DecimalMin(value = "0.0", inclusive = true, message = "length must be >= 0.0")
    private double length;

    @DecimalMin(value = "0.0", inclusive = true, message = "width must be >= 0.0")
    private double width;

    @DecimalMin(value = "0.0", inclusive = true, message = "height must be >= 0.0")
    private double height;

    @Size(max = 100, message = "packageType must be at most 100 characters")
    private String packageType;
    private boolean fragile;
    private boolean active;
    private boolean featured;
    private boolean digital;

    private ProductStatus status;

    private String description;

    @Size(max = 300)
    private String shortDescription;

    @Size(max = 100)
    private String sku;

    @Size(max = 200)
    private String slug;

    @Size(max = 160)
    private String metaDescription;

    @Size(max = 100)
    private String material;

    @Size(max = 100)
    private String countryOfOrigin;

    private Map<String, String> nameTranslations;
    private Map<String, String> descriptionTranslations;
    private Map<String, String> shortDescriptionTranslations;
    private Map<String, String> materialTranslations;
    private Map<String, String> countryOfOriginTranslations;
    private Map<String, String> manufacturerNameTranslations;

    @Min(value = 0)
    private int warrantyMonths;

    private List<String> tags;
}

