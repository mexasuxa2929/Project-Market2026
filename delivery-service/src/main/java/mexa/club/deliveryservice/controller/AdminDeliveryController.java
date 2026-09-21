package mexa.club.deliveryservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.deliveryservice.dto.ApiResponse;
import mexa.club.deliveryservice.dto.AssignCourierRequest;
import mexa.club.deliveryservice.dto.CourierRequest;
import mexa.club.deliveryservice.dto.CreateDeliveryRequest;
import mexa.club.deliveryservice.dto.DeliveryFeeUpdateRequest;
import mexa.club.deliveryservice.dto.DeliveryNoteRequest;
import mexa.club.deliveryservice.dto.DeliveryResponse;
import mexa.club.deliveryservice.dto.DeliveryZoneRequest;
import mexa.club.deliveryservice.dto.TrackingEventResponse;
import mexa.club.deliveryservice.entity.Courier;
import mexa.club.deliveryservice.entity.DeliveryStatus;
import mexa.club.deliveryservice.entity.DeliveryZone;
import mexa.club.deliveryservice.service.DeliveryService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Admin - Delivery", description = "Admin endpoints for managing deliveries, couriers, and delivery zones")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('DELIVERY_MANAGE')")
public class AdminDeliveryController {
    private final DeliveryService deliveryService;

    public AdminDeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    // ─── Deliveries ─────────────────────────────────────────────────────────────

    @PostMapping("/deliveries")
    public ApiResponse<DeliveryResponse> create(@Valid @RequestBody CreateDeliveryRequest request) {
        return ApiResponse.ok(deliveryService.create(request));
    }

    @GetMapping("/deliveries")
    public ApiResponse<List<DeliveryResponse>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) UUID courierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String search
    ) {
        boolean hasFilters = status != null || courierId != null || dateFrom != null
                || dateTo != null || region != null || (search != null && !search.isBlank());
        if (hasFilters) {
            return ApiResponse.ok(deliveryService.search(status, courierId, dateFrom, dateTo, region, search, page, size));
        }
        return ApiResponse.ok(deliveryService.all(page, size));
    }

    @GetMapping("/deliveries/{id}")
    public ApiResponse<DeliveryResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(deliveryService.getById(id));
    }

    @PutMapping("/deliveries/{id}/assign")
    public ApiResponse<DeliveryResponse> assign(@PathVariable UUID id, @Valid @RequestBody AssignCourierRequest request) {
        return ApiResponse.ok(deliveryService.assign(id, request));
    }

    @PostMapping("/deliveries/{id}/return")
    @Operation(summary = "Mark delivery as RETURNED", description = "Only FAILED deliveries can be returned.")
    public ApiResponse<DeliveryResponse> returnDelivery(
            @PathVariable UUID id,
            @RequestBody(required = false) DeliveryNoteRequest request
    ) {
        return ApiResponse.ok(deliveryService.returnDelivery(id, request != null ? request.note() : null));
    }

    @PutMapping("/deliveries/{id}/note")
    public ApiResponse<DeliveryResponse> updateNote(@PathVariable UUID id, @Valid @RequestBody DeliveryNoteRequest request) {
        return ApiResponse.ok(deliveryService.updateNote(id, request));
    }

    @PutMapping("/deliveries/{id}/fee")
    public ApiResponse<DeliveryResponse> updateFee(@PathVariable UUID id, @Valid @RequestBody DeliveryFeeUpdateRequest request) {
        return ApiResponse.ok(deliveryService.updateFee(id, request));
    }

    @GetMapping("/deliveries/{id}/events")
    public ApiResponse<List<TrackingEventResponse>> events(@PathVariable UUID id) {
        DeliveryResponse d = deliveryService.getById(id);
        return ApiResponse.ok(d.events());
    }

    @GetMapping("/deliveries/export")
    @Operation(summary = "Export deliveries as CSV")
    public ResponseEntity<Resource> export(
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
    ) {
        List<DeliveryResponse> list = deliveryService.search(status, null, dateFrom, dateTo, null, null, 0, Integer.MAX_VALUE);
        String csv = buildCsv(list);
        byte[] bytes = csv.getBytes();
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=deliveries.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(bytes.length)
                .body(resource);
    }

    // ─── Couriers ───────────────────────────────────────────────────────────────

    @GetMapping("/couriers")
    public ApiResponse<List<Courier>> couriers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String region
    ) {
        boolean hasFilters = (search != null && !search.isBlank()) || active != null || (region != null && !region.isBlank());
        if (hasFilters) {
            return ApiResponse.ok(deliveryService.searchCouriers(search, active, region));
        }
        return ApiResponse.ok(deliveryService.couriers());
    }

    @PostMapping("/couriers")
    public ApiResponse<Courier> createCourier(@Valid @RequestBody CourierRequest request) {
        return ApiResponse.ok(deliveryService.createCourier(request));
    }

    @PutMapping("/couriers/{id}")
    public ApiResponse<Courier> updateCourier(@PathVariable UUID id, @Valid @RequestBody CourierRequest request) {
        return ApiResponse.ok(deliveryService.updateCourier(id, request));
    }

    @GetMapping("/couriers/{id}")
    public ApiResponse<Courier> getCourier(@PathVariable UUID id) {
        return ApiResponse.ok(deliveryService.getCourierById(id));
    }

    @DeleteMapping("/couriers/{id}")
    public ApiResponse<Void> deleteCourier(@PathVariable UUID id) {
        deliveryService.deleteCourier(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/couriers/stats")
    public ApiResponse<?> courierStats() {
        return ApiResponse.ok(deliveryService.courierStats());
    }

    @GetMapping("/couriers/export")
    @Operation(summary = "Export couriers as CSV")
    public ResponseEntity<Resource> exportCouriers() {
        String csv = deliveryService.exportCouriers();
        byte[] bytes = csv.getBytes();
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=couriers.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(bytes.length)
                .body(resource);
    }

    // ─── Delivery Zones ─────────────────────────────────────────────────────────

    @GetMapping("/delivery-zones")
    public ApiResponse<List<DeliveryZone>> zones() {
        return ApiResponse.ok(deliveryService.allZones());
    }

    @PostMapping("/delivery-zones")
    public ApiResponse<DeliveryZone> createZone(@Valid @RequestBody DeliveryZoneRequest request) {
        return ApiResponse.ok(deliveryService.createZone(request));
    }

    @PutMapping("/delivery-zones/{id}")
    public ApiResponse<DeliveryZone> updateZone(@PathVariable UUID id, @Valid @RequestBody DeliveryZoneRequest request) {
        return ApiResponse.ok(deliveryService.updateZone(id, request));
    }

    @DeleteMapping("/delivery-zones/{id}")
    public ApiResponse<Void> deleteZone(@PathVariable UUID id) {
        deliveryService.deleteZone(id);
        return ApiResponse.ok(null);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private String buildCsv(List<DeliveryResponse> list) {
        String header = "TrackingCode,OrderId,Status,RecipientName,RecipientPhone,Region,District,DeliveryFee,CreatedAt\n";
        String rows = list.stream().map(d -> String.format("%s,%s,%s,%s,%s,%s,%s,%.2f,%s",
                csvEscape(d.trackingCode()), d.orderId(), d.status(),
                csvEscape(d.recipientName()), csvEscape(d.recipientPhone()),
                csvEscape(d.region()), csvEscape(d.district()),
                d.deliveryFee(), d.createdAt()
        )).collect(Collectors.joining("\n"));
        return header + rows;
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
