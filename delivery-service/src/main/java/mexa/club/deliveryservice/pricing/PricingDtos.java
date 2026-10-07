package mexa.club.deliveryservice.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Delivery pricing engine kirim/chiqish modellari. */
public final class PricingDtos {

    private PricingDtos() {}

    /** Engine kirimi — deterministik (bir xil kirim → bir xil natija). */
    public record PricingRequest(
            UUID zoneId,
            String region,
            String district,
            Double distanceKm,
            Double weightKg,
            BigDecimal orderValue,
            String deliveryType,
            LocalDateTime time,
            UUID orderId,
            boolean recordEvent
    ) {
        public static PricingRequest of(String region, String district, Double distanceKm,
                                        Double weightKg, BigDecimal orderValue, String deliveryType) {
            return new PricingRequest(null, region, district, distanceKm, weightKg,
                    orderValue, deliveryType, LocalDateTime.now(), null, false);
        }
    }

    /** Bitta surcharge qatori (breakdown uchun). */
    public record SurchargeLine(String name, String type, BigDecimal amount) {}

    /** Cart incentive tavsiyasi (frontendga qaytariladigan xabar bilan). */
    public record IncentiveInfo(
            BigDecimal currentOrderValue,
            BigDecimal nextThreshold,
            BigDecimal remainingAmount,
            BigDecimal currentDeliveryFee,
            BigDecimal deliveryFeeAfterThreshold,
            BigDecimal potentialSaving,
            String message
    ) {}

    /** To'liq pricing natijasi. */
    public record PricingQuote(
            BigDecimal baseFee,
            BigDecimal distanceFee,
            BigDecimal weightFee,
            List<SurchargeLine> surcharges,
            BigDecimal surchargeTotal,
            BigDecimal discount,
            BigDecimal finalFee,
            BigDecimal estimatedCost,
            BigDecimal margin,
            boolean freeDelivery,
            String appliedRule,
            String zoneName,
            String deliveryType,
            Integer estimatedMinutes,
            Integer estimatedDays,
            IncentiveInfo incentive,
            String message
    ) {}
}
