package mexa.club.geoservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.geoservice.dto.ApiResponse;
import mexa.club.geoservice.dto.GeoZoneRequest;
import mexa.club.geoservice.dto.GeoZoneResponse;
import mexa.club.geoservice.dto.PointInPolygonRequest;
import mexa.club.geoservice.entity.WarehouseLocation;
import mexa.club.geoservice.repository.WarehouseLocationRepository;
import mexa.club.geoservice.service.GeoZoneService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Admin - Geo Zones", description = "Admin endpoints for managing delivery zones with Google Maps polygons")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/geo-zones")
@PreAuthorize("hasAuthority('GEO_MANAGE')")
public class AdminGeoController {
    private final GeoZoneService service;
    private final WarehouseLocationRepository whLocationRepo;

    public AdminGeoController(GeoZoneService service, WarehouseLocationRepository whLocationRepo) {
        this.service = service;
        this.whLocationRepo = whLocationRepo;
    }

    @GetMapping
    public ApiResponse<List<GeoZoneResponse>> all() {
        return ApiResponse.ok(service.all());
    }

    @GetMapping("/by-warehouse/{warehouseId}")
    public ApiResponse<List<GeoZoneResponse>> byWarehouse(@PathVariable UUID warehouseId) {
        return ApiResponse.ok(service.allByWarehouse(warehouseId));
    }

    @GetMapping("/{id}")
    public ApiResponse<GeoZoneResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(service.getById(id));
    }

    @PostMapping
    public ApiResponse<GeoZoneResponse> create(@Valid @RequestBody GeoZoneRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<GeoZoneResponse> update(@PathVariable UUID id, @Valid @RequestBody GeoZoneRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/check-point")
    @Operation(summary = "Check which zones contain a given point")
    public ApiResponse<List<GeoZoneResponse>> checkPoint(@Valid @RequestBody PointInPolygonRequest request) {
        return ApiResponse.ok(service.checkPoint(request));
    }

    @PutMapping("/warehouse/{warehouseId}/position")
    @Operation(summary = "Set warehouse location on map")
    public ApiResponse<Map<String, Object>> setWarehousePosition(
            @PathVariable UUID warehouseId,
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        WarehouseLocation loc = whLocationRepo.findById(warehouseId).orElse(new WarehouseLocation());
        loc.setWarehouseId(warehouseId);
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        loc.setUpdatedAt(LocalDateTime.now());
        whLocationRepo.save(loc);
        return ApiResponse.ok(Map.of("warehouseId", warehouseId.toString(), "latitude", latitude, "longitude", longitude));
    }

    @GetMapping("/warehouse/{warehouseId}/position")
    @Operation(summary = "Get warehouse location")
    public ApiResponse<?> getWarehousePosition(@PathVariable UUID warehouseId) {
        var opt = whLocationRepo.findById(warehouseId);
        if (opt.isEmpty()) {
            return ApiResponse.ok(null);
        }
        WarehouseLocation loc = opt.get();
        return ApiResponse.ok(Map.of("warehouseId", warehouseId.toString(), "latitude", loc.getLatitude(), "longitude", loc.getLongitude()));
    }

    @DeleteMapping("/warehouse/{warehouseId}/position")
    @Operation(summary = "Delete warehouse location from map")
    public ApiResponse<Void> deleteWarehousePosition(@PathVariable UUID warehouseId) {
        whLocationRepo.deleteById(warehouseId);
        return ApiResponse.ok(null);
    }
}
