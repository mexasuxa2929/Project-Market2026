package mexa.club.orderservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** delivery-service /api/delivery/pricing/quote javobi (faqat preview uchun kerakli qismlar). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryQuotePayload(BigDecimal finalFee, Boolean freeDelivery, QuoteIncentive incentive) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuoteIncentive(String message) {}
}
