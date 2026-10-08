package mexa.club.deliveryservice.entity;

public enum DeliveryStatus {
    PENDING,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    FAILED,
    RETURNED,
    /** H10: order bekor qilinganda (order-service chaqiruvi bilan). */
    CANCELLED
}
