package mexa.club.deliveryservice.dto;

import java.util.UUID;

/**
 * Omborga biriktirilgan kuryer bo'yicha yetkazmalar statistikasi
 * (Kuryerlar tabini boy ko'rsatish uchun).
 */
public record CourierWarehouseStatsResponse(
        UUID courierId,
        long total,
        long active,
        long delivered,
        long failed,
        long returned,
        long today
) {}
