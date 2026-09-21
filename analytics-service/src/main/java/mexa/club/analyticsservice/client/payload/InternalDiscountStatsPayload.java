package mexa.club.analyticsservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalDiscountStatsPayload(
        long activePromotions,
        BigDecimal totalDiscountGiven,
        List<TopPromotionPayload> topPromotions
) {}
