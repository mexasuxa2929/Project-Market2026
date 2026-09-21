package mexa.club.discountservice.dto;

import mexa.club.discountservice.entity.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record PromotionResponse(
        UUID id,
        String code,
        String name,
        String description,
        PromotionType type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer usageLimit,
        Integer usageCount,
        Integer perUserLimit,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Set<UUID> restrictedProductIds,
        Set<UUID> restrictedCategoryIds
) {}
