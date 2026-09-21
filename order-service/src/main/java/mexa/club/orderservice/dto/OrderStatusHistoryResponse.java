package mexa.club.orderservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusHistoryResponse(
        UUID id,
        UUID orderId,
        String fromStatus,
        String toStatus,
        UUID changedBy,
        String reason,
        LocalDateTime createdAt
) {}
