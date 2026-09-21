package mexa.club.orderservice.client.discount;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscountApplyPayload(
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        Boolean freeShipping
) {}
