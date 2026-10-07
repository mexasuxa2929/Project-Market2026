package mexa.club.paymentservice.dto;

import java.util.UUID;

public record RefundResponse(UUID orderId, String transactionId, String refundStatus) {}
