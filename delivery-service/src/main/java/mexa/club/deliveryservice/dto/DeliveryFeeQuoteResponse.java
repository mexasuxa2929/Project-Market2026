package mexa.club.deliveryservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Yetkazib berish narxi tafsiloti:
 * fee = baseFee + distanceFee + weightFee (freeShipping bo'lsa 0).
 */
public record DeliveryFeeQuoteResponse(
        BigDecimal fee,
        Integer estimatedDays,
        BigDecimal baseFee,
        Double distanceKm,
        BigDecimal distanceFee,
        Double weightKg,
        BigDecimal weightFee,
        boolean freeShipping,
        BigDecimal freeAbove,
        UUID zoneId
) {}
