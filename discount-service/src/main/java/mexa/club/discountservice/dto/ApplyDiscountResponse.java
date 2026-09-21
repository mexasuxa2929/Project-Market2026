package mexa.club.discountservice.dto;

import mexa.club.discountservice.entity.PromotionType;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyDiscountResponse(
        UUID promotionId,
        String code,
        PromotionType discountType,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        boolean freeShipping,
        String message
) {}
