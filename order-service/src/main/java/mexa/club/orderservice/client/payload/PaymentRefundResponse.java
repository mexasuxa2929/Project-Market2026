package mexa.club.orderservice.client.payload;

import java.util.UUID;

public record PaymentRefundResponse(UUID orderId, String transactionId, String refundStatus) {}
