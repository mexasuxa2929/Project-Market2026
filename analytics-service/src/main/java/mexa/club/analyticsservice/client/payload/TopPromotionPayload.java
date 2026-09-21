package mexa.club.analyticsservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TopPromotionPayload(String code, long usageCount, BigDecimal totalDiscount) {}
