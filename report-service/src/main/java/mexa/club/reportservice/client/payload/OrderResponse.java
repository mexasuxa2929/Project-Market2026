package mexa.club.reportservice.client.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID userId,
        String status,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {}
