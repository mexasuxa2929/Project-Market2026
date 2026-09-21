package mexa.club.discountservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import mexa.club.discountservice.entity.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record CreatePromotionRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull PromotionType type,
        @NotNull @PositiveOrZero BigDecimal value,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer usageLimit,
        Integer perUserLimit,
        @NotNull LocalDateTime startsAt,
        @Future LocalDateTime endsAt,
        Boolean active,
        Set<UUID> restrictedProductIds,
        Set<UUID> restrictedCategoryIds
) {}
