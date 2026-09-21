package mexa.club.analyticsservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderApiEnvelope(Boolean success, InternalOrderStatsPayload data, String message) {}
