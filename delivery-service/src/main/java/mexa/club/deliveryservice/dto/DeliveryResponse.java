package mexa.club.deliveryservice.dto;

import mexa.club.deliveryservice.entity.DeliveryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID orderId,
        UUID courierId,
        DeliveryStatus status,
        UUID fromWarehouseId,
        String deliveryAddress,
        String recipientName,
        String recipientPhone,
        String region,
        String district,
        LocalDate estimatedDelivery,
        LocalDateTime actualDelivery,
        BigDecimal deliveryFee,
        String trackingCode,
        String note,
        String failReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TrackingEventResponse> events
) {}
