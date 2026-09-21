package mexa.club.geoservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GeoZoneResponse(
        UUID id,
        String name,
        String region,
        String district,
        String polygon,
        String centerLat,
        String centerLng,
        Integer zoomLevel,
        BigDecimal fee,
        Integer estimatedDays,
        String color,
        boolean active,
        UUID warehouseId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
