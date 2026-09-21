package mexa.club.geoservice.service;

import mexa.club.geoservice.dto.GeoZoneResponse;
import mexa.club.geoservice.entity.GeoZone;
import org.springframework.stereotype.Component;

@Component
public class GeoZoneMapper {
    public GeoZoneResponse toResponse(GeoZone z) {
        return new GeoZoneResponse(
                z.getId(), z.getName(), z.getRegion(), z.getDistrict(),
                z.getPolygon(), z.getCenterLat(), z.getCenterLng(),
                z.getZoomLevel(), z.getFee(), z.getEstimatedDays(),
                z.getColor(), z.isActive(),
                z.getWarehouseId(),
                z.getCreatedAt(), z.getUpdatedAt()
        );
    }
}
