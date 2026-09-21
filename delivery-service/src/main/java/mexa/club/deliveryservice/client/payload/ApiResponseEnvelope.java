package mexa.club.deliveryservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiResponseEnvelope<T>(boolean success, T data, String message) {
}
