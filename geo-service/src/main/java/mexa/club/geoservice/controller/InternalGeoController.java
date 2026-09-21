package mexa.club.geoservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.geoservice.dto.ApiResponse;
import mexa.club.geoservice.dto.GeoZoneResponse;
import mexa.club.geoservice.dto.PointInPolygonRequest;
import mexa.club.geoservice.repository.WarehouseLocationRepository;
import mexa.club.geoservice.service.GeoZoneService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Internal - Geo Zones", description = "Internal endpoints for other services")
@RestController
@RequestMapping("/internal/geo-zones")
public class InternalGeoController {
    private final GeoZoneService service;
    private final WarehouseLocationRepository whLocationRepo;

    public InternalGeoController(GeoZoneService service, WarehouseLocationRepository whLocationRepo) {
        this.service = service;
        this.whLocationRepo = whLocationRepo;
    }

    @GetMapping("/check-point")
    public ApiResponse<List<GeoZoneResponse>> checkPoint(
            @RequestParam String lat,
            @RequestParam String lng
    ) {
        return ApiResponse.ok(service.checkPoint(new PointInPolygonRequest(lat, lng)));
    }

    @GetMapping("/active")
    @Operation(summary = "Active coverage zones (for mobile map)")
    public ApiResponse<List<GeoZoneResponse>> active() {
        return ApiResponse.ok(service.allActive());
    }

    @DeleteMapping("/by-warehouse/{warehouseId}")
    public ApiResponse<Void> deleteByWarehouse(@PathVariable UUID warehouseId) {
        service.deleteByWarehouseId(warehouseId);
        whLocationRepo.deleteById(warehouseId);
        return ApiResponse.ok(null);
    }
}
