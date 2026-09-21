package mexa.club.shopservice.dto;

import java.time.Instant;
import java.util.UUID;

/** Ommaviy ko'rish uchun — userId yashirilgan, username ko'rsatiladi */
public record PublicReviewResponse(
        UUID id,
        String username,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {}
