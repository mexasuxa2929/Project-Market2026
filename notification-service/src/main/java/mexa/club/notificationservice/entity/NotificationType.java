package mexa.club.notificationservice.entity;

public enum NotificationType {
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    LOW_STOCK_ALERT,
    WELCOME,
    PASSWORD_RESET,
    /** delivery-service: kuryer biriktirildi */
    DELIVERY_ASSIGNED,
    /** delivery-service: holat o‘zgardi */
    DELIVERY_STATUS_UPDATED,
    /** delivery-service: yetkazildi */
    DELIVERY_COMPLETED,
    /** delivery-service: muvaffaqiyatsiz */
    DELIVERY_FAILED
}
