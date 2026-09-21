package mexa.club.discountservice.dto;

import mexa.club.discountservice.entity.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Partial update: {@code null} fields are left unchanged.
 */
public record UpdatePromotionRequest(
        String name,
        String description,
        PromotionType type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer usageLimit,
        Integer perUserLimit,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active,
        Set<UUID> restrictedProductIds,
        Set<UUID> restrictedCategoryIds
) {}
