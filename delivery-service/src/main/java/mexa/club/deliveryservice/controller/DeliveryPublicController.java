package mexa.club.deliveryservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.deliveryservice.dto.ApiResponse;
import mexa.club.deliveryservice.dto.DeliveryFeeResponse;
import mexa.club.deliveryservice.dto.DeliveryResponse;
import mexa.club.deliveryservice.service.DeliveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Delivery (Public)", description = "Public endpoints for tracking deliveries and calculating delivery fees")
@RestController
@RequestMapping
public class DeliveryPublicController {
    private final DeliveryService deliveryService;

    public DeliveryPublicController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Operation(
        summary = "Track delivery by tracking code",
        description = "Returns the current status and details of a delivery identified by its tracking code. No authentication required."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery found and returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery not found for the given tracking code")
    })
    @GetMapping("/api/deliveries/track/{trackingCode}")
    public ApiResponse<DeliveryResponse> track(
            @Parameter(description = "Delivery tracking code", required = true)
            @PathVariable String trackingCode) {
        return ApiResponse.ok(deliveryService.getByTracking(trackingCode));
    }

    @Operation(
        summary = "Get delivery by order ID",
        description = "Returns the delivery associated with the specified order UUID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery found and returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No delivery found for the given order ID")
    })
    @GetMapping("/api/deliveries/order/{orderId}")
    public ApiResponse<DeliveryResponse> byOrder(
            @Parameter(description = "UUID of the order to look up delivery for", required = true)
            @PathVariable UUID orderId) {
        return ApiResponse.ok(deliveryService.getByOrderId(orderId));
    }

    @Operation(
        summary = "Calculate delivery fee",
        description = "Returns the delivery fee for the given region and district combination. Used by the checkout flow before an order is placed."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery fee calculated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid region or district")
    })
    @GetMapping("/api/delivery/fee")
    public ApiResponse<DeliveryFeeResponse> fee(
            @Parameter(description = "Region name or code", required = true)
            @RequestParam String region,
            @Parameter(description = "District name or code within the region", required = true)
            @RequestParam String district) {
        return ApiResponse.ok(deliveryService.calculateFee(region, district));
    }
}
