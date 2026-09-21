package mexa.club.shopservice.dto.order;

public record OrderServiceEnvelope<T>(
        boolean success,
        T data,
        String message
) {
}
