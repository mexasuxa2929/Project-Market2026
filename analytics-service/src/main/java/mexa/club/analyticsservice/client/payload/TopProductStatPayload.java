package mexa.club.analyticsservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TopProductStatPayload(UUID productId, String productName, long count, BigDecimal revenue) {}
