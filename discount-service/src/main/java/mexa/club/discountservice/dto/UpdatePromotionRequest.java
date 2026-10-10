package mexa.club.discountservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import mexa.club.discountservice.entity.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Partial update: {@code null} fields are left unchanged.
 * M-28: cheklovlar — noto'g'ri qiymatlar DB'gacha ketmaydi.
 */
public record UpdatePromotionRequest(
        @Size(max = 150, message = "name must be at most 150 chars")
        String name,
        @Size(max = 1000, message = "description must be at most 1000 chars")
        String description,
        PromotionType type,
        @DecimalMin(value = "0", message = "value must be >= 0")
        BigDecimal value,
        @DecimalMin(value = "0", message = "minOrderAmount must be >= 0")
        BigDecimal minOrderAmount,
        @DecimalMin(value = "0", message = "maxDiscountAmount must be >= 0")
        BigDecimal maxDiscountAmount,
        @Min(value = 0, message = "usageLimit must be >= 0")
        Integer usageLimit,
        @Min(value = 0, message = "perUserLimit must be >= 0")
        Integer perUserLimit,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active,
        Set<UUID> restrictedProductIds,
        Set<UUID> restrictedCategoryIds
) {}
