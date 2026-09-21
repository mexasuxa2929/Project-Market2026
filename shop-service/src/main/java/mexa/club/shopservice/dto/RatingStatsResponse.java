package mexa.club.shopservice.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Mahsulot reytingi statistikasi.
 * distribution: { 1: 5, 2: 3, 3: 10, 4: 20, 5: 42 } — har yulduz uchun nechta review
 */
public record RatingStatsResponse(
        UUID productId,
        double avgRating,
        long reviewCount,
        Map<Integer, Long> distribution
) {}
