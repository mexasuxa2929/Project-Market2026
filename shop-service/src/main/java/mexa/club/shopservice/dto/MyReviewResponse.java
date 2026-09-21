package mexa.club.shopservice.dto;

import java.time.Instant;
import java.util.UUID;

public record MyReviewResponse(
        UUID id,
        UUID productId,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        boolean canEdit   // sotib olingan — tahrirlash mumkin
) {}
