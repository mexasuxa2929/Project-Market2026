package mexa.club.productservice.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64, unique = true)
    private String barcode;

    /**
     * Rang nomi (masalan "Qora", "Oq"). Har bir rang — alohida Product yozuvi.
     * Variantlar olib tashlandi: endi har rang mustaqil mahsulot hisoblanadi, o'z barcode/narxi bilan.
     */
    @Size(max = 60)
    @Column(length = 60)
    private String color;

    /** Rang HEX kodi (masalan "#FF0000"). */
    @Size(max = 20)
    @Column(length = 20)
    private String colorCode;

    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> nameTranslations = new LinkedHashMap<>();

    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> descriptionTranslations = new LinkedHashMap<>();

    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> shortDescriptionTranslations = new LinkedHashMap<>();

    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> materialTranslations = new LinkedHashMap<>();

    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> countryOfOriginTranslations = new LinkedHashMap<>();

    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> manufacturerNameTranslations = new LinkedHashMap<>();

    /**
     * Bir xil mahsulotning turli rangdagi nusxalarini bog'lash uchun guruh identifikatori.
     * Bir guruhdagi barcha Product yozuvlari bitta "mahsulot" sifatida ko'rsatiladi (rang tanlovi bilan),
     * lekin har biri o'z barcode/narx/sklad qoldig'iga ega.
     */
    private UUID groupId;

    /**
     * Rang guruhidagi "vakil" (representative) belgisi. Har bir {@code groupId} guruhida aynan
     * bitta yozuv {@code true} bo'ladi va ro'yxat endpointida shu vakil ko'rsatiladi; qolgan rang
     * nusxalari {@code false} bo'lib, faqat edit oynasidagi rang guruhi orqali ochiladi.
     * Guruhsiz (standalone) mahsulotlar ham {@code true} hisoblanadi.
     */
    private boolean primaryVariant = true;

    private UUID categoryId;

    private UUID brandId;

    private UUID manufacturerId;

    @Size(max = 100)
    @Column(length = 100)
    private String manufacturerName;

    @NotBlank
    @Size(max = 32)
    @Column(nullable = false, length = 32)
    private String unit;

    private int leadTimeDays;

    /**
     * Mahsulot yetkazish muddati (har mahsulotga unikal):
     * min — omborda bor holatda necha kunda yetkaziladi,
     * max — zavoddan zakaz berilganda necha kunda omborga kelib mijozga yetkaziladi.
     */
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 300)
    private String shortDescription;

    @Size(max = 100)
    private String sku;

    @Size(max = 200)
    @Column(unique = true, length = 200)
    private String slug;

    @Size(max = 160)
    private String metaDescription;

    @Size(max = 100)
    private String material;

    @Size(max = 100)
    private String countryOfOrigin;

    private int warrantyMonths;

    private UUID createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @BatchSize(size = 32)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_image", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "relative_path", length = 512)
    private List<String> imagePaths = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_to_tag",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private java.util.Set<ProductTag> tags = new LinkedHashSet<>();

    @PrePersist
    private void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

