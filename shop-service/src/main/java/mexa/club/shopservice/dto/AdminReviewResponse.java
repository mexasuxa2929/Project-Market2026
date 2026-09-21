package mexa.club.shopservice.dto;

import java.time.Instant;
import java.util.UUID;

/** Admin uchun to'liq review ma'lumoti */
public record AdminReviewResponse(
        UUID    id,
        UUID    userId,
        UUID    productId,
        String  username,
        int     rating,
        String  comment,
        Instant createdAt,
        Instant updatedAt
) {}
