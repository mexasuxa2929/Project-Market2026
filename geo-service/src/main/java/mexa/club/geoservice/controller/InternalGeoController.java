package mexa.club.geoservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.geoservice.dto.ApiResponse;
import mexa.club.geoservice.dto.GeoZoneResponse;
import mexa.club.geoservice.dto.PointInPolygonRequest;
import mexa.club.geoservice.exception.GeoException;
import mexa.club.geoservice.repository.WarehouseLocationRepository;
import mexa.club.geoservice.service.GeoZoneService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Tag(
        name = "Internal - Geo Zones",
        description = "Internal endpoints for other services. All requests must include the X-Internal-Secret header."
)
@RestController
@RequestMapping("/internal/geo-zones")
public class InternalGeoController {
    private final GeoZoneService service;
    private final WarehouseLocationRepository whLocationRepo;
    private final String internalSecret;

    public InternalGeoController(
            GeoZoneService service,
            WarehouseLocationRepository whLocationRepo,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.service = service;
        this.whLocationRepo = whLocationRepo;
        this.internalSecret = internalSecret;
    }

    @GetMapping("/check-point")
    public ApiResponse<List<GeoZoneResponse>> checkPoint(
            @RequestParam String lat,
            @RequestParam String lng,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret
    ) {
        ensureInternalSecret(secret);
        return ApiResponse.ok(service.checkPoint(new PointInPolygonRequest(lat, lng)));
    }

    @GetMapping("/active")
    @Operation(summary = "Active coverage zones (for mobile map)")
    public ApiResponse<List<GeoZoneResponse>> active(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret
    ) {
        ensureInternalSecret(secret);
        return ApiResponse.ok(service.allActive());
    }

    @DeleteMapping("/by-warehouse/{warehouseId}")
    public ApiResponse<Void> deleteByWarehouse(
            @PathVariable UUID warehouseId,
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret
    ) {
        ensureInternalSecret(secret);
        service.deleteByWarehouseId(warehouseId);
        whLocationRepo.deleteById(warehouseId);
        return ApiResponse.ok(null);
    }

    private void ensureInternalSecret(String secret) {
        if (secret == null || secret.isBlank() || !matches(secret)) {
            throw new GeoException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Invalid or missing X-Internal-Secret header");
        }
    }

    private boolean matches(String secret) {
        return MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                internalSecret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
