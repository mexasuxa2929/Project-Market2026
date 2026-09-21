package mexa.club.geoservice.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record GeoZoneRequest(
        @NotBlank String name,
        String region,
        String district,
        @NotBlank String polygon,
        String centerLat,
        String centerLng,
        Integer zoomLevel,
        BigDecimal fee,
        Integer estimatedDays,
        String color,
        boolean active,
        String warehouseId
) {}
