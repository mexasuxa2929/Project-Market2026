package mexa.club.deliveryservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.deliveryservice.dto.ApiResponse;
import mexa.club.deliveryservice.dto.FailDeliveryRequest;
import mexa.club.deliveryservice.dto.DeliveryResponse;
import mexa.club.deliveryservice.dto.UpdateDeliveryStatusRequest;
import mexa.club.deliveryservice.exception.DeliveryException;
import mexa.club.deliveryservice.security.JwtUserPrincipal;
import mexa.club.deliveryservice.service.DeliveryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Courier - Deliveries", description = "Endpoints for couriers to view and update the status of their assigned deliveries")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/courier/deliveries")
@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('DELIVERY_STATUS_UPDATE')")
public class CourierDeliveryController {
    private final DeliveryService deliveryService;

    public CourierDeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Operation(
        summary = "Get my assigned deliveries",
        description = "Returns all deliveries currently assigned to the authenticated courier."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deliveries retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "DELIVERY_STATUS_UPDATE authority required")
    })
    @GetMapping
    public ApiResponse<List<DeliveryResponse>> my(Authentication authentication) {
        return ApiResponse.ok(deliveryService.courierDeliveries(userId(authentication)));
    }

    @Operation(
        summary = "Update delivery status",
        description = "Updates the status of the specified delivery. The authenticated courier must be the one assigned to this delivery."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not assigned to this delivery or insufficient authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    @PutMapping("/{id}/status")
    public ApiResponse<DeliveryResponse> updateStatus(
            @Parameter(description = "UUID of the delivery to update", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryStatusRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(deliveryService.updateCourierStatus(id, userId(authentication), request));
    }

    @Operation(
        summary = "Mark delivery as completed",
        description = "Marks the specified delivery as successfully delivered. The authenticated courier must be assigned to this delivery."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery marked as completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not assigned to this delivery"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    @PostMapping("/{id}/complete")
    public ApiResponse<DeliveryResponse> complete(
            @Parameter(description = "UUID of the delivery to complete", required = true)
            @PathVariable UUID id,
            Authentication authentication) {
        return ApiResponse.ok(deliveryService.complete(id, userId(authentication)));
    }

    @Operation(
        summary = "Mark delivery as failed",
        description = "Marks the specified delivery as failed with a mandatory reason. The authenticated courier must be assigned to this delivery."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery marked as failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Failure reason is required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not assigned to this delivery"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    @PostMapping("/{id}/fail")
    public ApiResponse<DeliveryResponse> fail(
            @Parameter(description = "UUID of the delivery that failed", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody FailDeliveryRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(deliveryService.fail(id, userId(authentication), request.reason()));
    }

    private UUID userId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof JwtUserPrincipal principal) {
            return principal.userId();
        }
        throw new DeliveryException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid principal");
    }
}
