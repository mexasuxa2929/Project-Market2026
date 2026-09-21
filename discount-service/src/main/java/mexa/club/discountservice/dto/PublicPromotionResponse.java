package mexa.club.discountservice.dto;

import mexa.club.discountservice.entity.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PublicPromotionResponse(
        UUID id,
        String code,
        String name,
        String description,
        PromotionType type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}
