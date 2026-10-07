package mexa.club.orderservice.client.payload;

import java.util.UUID;

public record PaymentRefundRequest(UUID orderId, String reason) {}
