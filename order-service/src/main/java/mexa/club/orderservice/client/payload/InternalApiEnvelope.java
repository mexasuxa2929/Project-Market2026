package mexa.club.orderservice.client.payload;

/**
 * Ichki servislar javob konverti: {"success":true,"data":{...},"message":null}.
 * Barcha internal endpoint'lar shu shaklda javob qaytaradi.
 */
public record InternalApiEnvelope<T>(boolean success, T data, String message) {
}
