package mexa.club.paymentservice.dto;

import java.util.UUID;

public record RefundRequest(UUID orderId, String reason) {}
