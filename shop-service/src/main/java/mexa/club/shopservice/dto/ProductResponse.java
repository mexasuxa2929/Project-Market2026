package mexa.club.shopservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String barcode,
        String sku,
        String slug,
        String color,
        String colorCode,
        UUID groupId,

        // Kategoriya / Brand / Ishlab chiqaruvchi
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        UUID manufacturerId,
        String manufacturerName,

        // Tavsif
        String description,
        String shortDescription,
        String metaDescription,

        // Fizik xususiyatlar
        String unit,
        int leadTimeDays,
        Integer deliveryDaysMin,
        Integer deliveryDaysMax,
        int minStock,
        double weight,
        double length,
        double width,
        double height,
        String packageType,
        boolean fragile,

        // Material / Kafolat
        String material,
        String countryOfOrigin,
        int warrantyMonths,

        // Status
        boolean active,
        boolean featured,
        boolean digital,
        String status,

        // Tarjimalar
        Map<String, String> nameTranslations,
        Map<String, String> descriptionTranslations,
        Map<String, String> shortDescriptionTranslations,
        Map<String, String> materialTranslations,
        Map<String, String> countryOfOriginTranslations,
        Map<String, String> manufacturerNameTranslations,

        // Ranglar guruhi (bir xil mahsulotning boshqa rangdagi nusxalari), Rasmlar, Teglar
        List<ColorSiblingDto> siblingColors,
        List<String> imageUrls,
        List<String> tags,

        // Narx / Stock (product-service tomonidan boyitiladi)
        BigDecimal basePrice,
        Integer discountPercent,
        Integer totalStock,
        Boolean inStock,

        // Reyting (product-service tomonidan boyitiladi)
        Double avgRating,
        Long reviewCount,

        // Vaqt
        LocalDateTime createdAt
) {
    /**
     * Bir guruhdagi (groupId) boshqa rangdagi mahsulot haqida qisqa ma'lumot.
     * product-service'dagi ProductColorSiblingResponse'ning mahalliy ko'rinishi —
     * har bir rang mustaqil Product hisoblanadi, bu DTO faqat ko'rsatish (rang swatch) uchun.
     */
    public record ColorSiblingDto(
            UUID id,
            String color,
            String colorCode,
            String barcode,
            String imageUrl,
            boolean active,
            boolean isCurrent
    ) {}
}
