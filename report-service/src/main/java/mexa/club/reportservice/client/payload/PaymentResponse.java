package mexa.club.reportservice.client.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        String provider,
        String status,
        BigDecimal amount,
        String providerTransactionId,
        LocalDateTime createdAt
) {}
