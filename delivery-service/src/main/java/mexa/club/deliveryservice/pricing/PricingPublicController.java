package mexa.club.deliveryservice.pricing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mexa.club.deliveryservice.dto.ApiResponse;
import mexa.club.deliveryservice.pricing.PricingDtos.IncentiveInfo;
import mexa.club.deliveryservice.pricing.PricingDtos.PricingQuote;
import mexa.club.deliveryservice.pricing.PricingDtos.PricingRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Public: pricing quote va cart incentive (checkout / mobile uchun). */
@Tag(name = "Delivery Pricing", description = "Delivery pricing quote va cart incentive endpoints")
@RestController
@RequestMapping("/api/delivery/pricing")
@RequiredArgsConstructor
public class PricingPublicController {

    private final DeliveryPricingService pricingService;

    @Operation(summary = "Full delivery price quote",
            description = "fee = base + distance*perKm + weight*perKg (min/max clamp), keyin surcharges va "
                    + "free delivery/incentive chegirmalari. Natijada incentive xabari ham qaytariladi.")
    @GetMapping("/quote")
    public ApiResponse<PricingQuote> quote(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) Double distanceKm,
            @RequestParam(required = false) Double weightKg,
            @RequestParam(required = false) BigDecimal orderValue,
            @RequestParam(required = false) String deliveryType,
            @RequestParam(required = false) String time) {
        LocalDateTime t = parseTime(time);
        // D-H4: user ko'rayotgan quote ham exposure sifatida yoziladi.
        return ApiResponse.ok(pricingService.calculate(new PricingRequest(
                zoneId, region, district, distanceKm, weightKg, orderValue, deliveryType, t, null, true, "QUOTE")));
    }

    @Operation(summary = "Cart incentive (kattaroq buyurtmaga undash)",
            description = "Joriy savat uchun keyingi threshold, qolgan summa, tejash va mijozga xabar.")
    @GetMapping("/incentive")
    public ApiResponse<Map<String, Object>> incentive(
            @RequestParam BigDecimal orderValue,
            @RequestParam(required = false) BigDecimal currentDeliveryFee,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) String deliveryType) {
        PricingQuote q = pricingService.calculate(new PricingRequest(
                zoneId, region, district, null, null, orderValue, deliveryType,
                LocalDateTime.now(), null, true, "INCENTIVE"));
        IncentiveInfo info = q.incentive();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("currentOrderValue", orderValue);
        out.put("currentDeliveryFee", currentDeliveryFee != null ? currentDeliveryFee : q.finalFee());
        if (info == null) {
            out.put("nextThreshold", null);
            out.put("remainingAmount", BigDecimal.ZERO);
            out.put("deliveryFeeAfterThreshold", q.finalFee());
            out.put("potentialSaving", BigDecimal.ZERO);
            out.put("message", null);
        } else {
            out.put("nextThreshold", info.nextThreshold());
            out.put("remainingAmount", info.remainingAmount());
            out.put("deliveryFeeAfterThreshold", info.deliveryFeeAfterThreshold());
            out.put("potentialSaving", info.potentialSaving());
            out.put("message", info.message());
        }
        out.put("finalFee", q.finalFee());
        out.put("freeDelivery", q.freeDelivery());
        return ApiResponse.ok(out);
    }

    private static LocalDateTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(raw.replace(" ", "T"));
            } catch (Exception ignored) {
                return LocalDateTime.now();
            }
        }
    }
}
