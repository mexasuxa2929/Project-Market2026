package mexa.club.deliveryservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderInternalData(UUID id, UUID userId, String orderNumber) {
}
