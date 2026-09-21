package mexa.club.orderservice.client.discount;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscountEnvelope<T>(
        boolean success,
        T data,
        String message,
        String code
) {}
