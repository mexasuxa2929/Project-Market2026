package mexa.club.shopservice.dto;

import java.time.Instant;
import java.util.UUID;

public record WishlistEntryResponse(
        UUID productId,
        Instant addedAt,
        String productName,
        String imageUrl
) {
    /** Mahsulot o'chirilgan bo'lsa ham ro'yxat buzilmasligi uchun yalang'och variant. */
    public static WishlistEntryResponse bare(UUID productId, Instant addedAt) {
        return new WishlistEntryResponse(productId, addedAt, null, null);
    }
}
