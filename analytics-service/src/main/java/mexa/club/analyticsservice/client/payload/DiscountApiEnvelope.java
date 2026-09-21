package mexa.club.analyticsservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscountApiEnvelope(
        boolean success,
        InternalDiscountStatsPayload data,
        String message,
        String code
) {}
