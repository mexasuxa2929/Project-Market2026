package mexa.club.productservice.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BrandResponse(
        UUID id,
        String name,
        String description,
        Map<String, String> nameTranslations,
        Map<String, String> descriptionTranslations,
        boolean active,
        String logoUrl,
        Instant createdAt,
        Instant updatedAt
) {}
