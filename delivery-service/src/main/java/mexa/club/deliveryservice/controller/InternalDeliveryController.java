package mexa.club.deliveryservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.deliveryservice.dto.ApiResponse;
import mexa.club.deliveryservice.dto.CreateDeliveryRequest;
import mexa.club.deliveryservice.dto.DeliveryResponse;
import mexa.club.deliveryservice.exception.DeliveryException;
import mexa.club.deliveryservice.service.DeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal - Delivery", description = "Internal endpoint for creating deliveries from other microservices. Protected by X-Internal-Secret header.")
@RestController
@RequestMapping("/internal/deliveries")
public class InternalDeliveryController {
    private final DeliveryService deliveryService;
    private final String internalSecret;

    public InternalDeliveryController(DeliveryService deliveryService, @Value("${app.internal-secret}") String internalSecret) {
        this.deliveryService = deliveryService;
        this.internalSecret = internalSecret;
    }

    @Operation(
        summary = "Create delivery (internal)",
        description = "Creates a delivery record from an internal service call (e.g. order-service after an order is confirmed). Requires the X-Internal-Secret header."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid delivery request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Invalid or missing X-Internal-Secret header")
    })
    @PostMapping("/create")
    public ApiResponse<DeliveryResponse> create(
            @Parameter(description = "Internal service secret key", required = true)
            @RequestHeader("X-Internal-Secret") String secret,
            @Valid @RequestBody CreateDeliveryRequest request
    ) {
        if (secret == null || !secret.equals(internalSecret)) {
            throw new DeliveryException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Invalid internal secret");
        }
        return ApiResponse.ok(deliveryService.create(request));
    }
}
