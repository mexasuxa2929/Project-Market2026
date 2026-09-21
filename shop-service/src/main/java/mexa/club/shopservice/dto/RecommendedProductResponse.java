package mexa.club.shopservice.dto;

import java.util.UUID;

/**
 * Rekomendatsiya qilingan mahsulot — reyting ma'lumotlari bilan boyitilgan.
 */
public record RecommendedProductResponse(
        UUID productId,
        String name,
        String thumbnail,
        String categoryName,
        String brandName,
        Double basePrice,
        double avgRating,
        long reviewCount
) {}
