package mexa.club.geoservice.dto;

import jakarta.validation.constraints.NotBlank;

public record PointInPolygonRequest(
        @NotBlank String lat,
        @NotBlank String lng
) {}
