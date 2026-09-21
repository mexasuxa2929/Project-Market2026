package mexa.club.shopservice.dto;

import java.util.List;
import java.util.UUID;

/**
 * Bosh sahifa uchun bitta bundle javob — 4 ta alohida round-trip o'rniga 1 ta.
 * flash alohida berilmaydi: client trending ichidan (discountPercent > 0) o'zi hisoblaydi,
 * shuning uchun payload'da takroriy mahsulotlar bo'lmaydi.
 */
public record HomeResponse(
        List<ProductResponse> trending,
        boolean trendingHasMore,
        UUID trendingNextCursor,
        List<RecommendedProductResponse> recommended,
        List<HomeBrandResponse> brands,
        List<HomeCategoryResponse> categories
) {}
