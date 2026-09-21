package mexa.club.orderservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.orderservice.dto.ApiResponse;
import mexa.club.orderservice.dto.InternalOrderStatsResponse;
import mexa.club.orderservice.dto.OrderResponse;
import mexa.club.orderservice.dto.ProductUsageCheckResponse;
import mexa.club.orderservice.exception.OrderServiceException;
import mexa.club.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/internal/orders")
@Tag(name = "Internal - Orders", description = "Service-to-service endpoints for the order domain. All requests must include the X-Internal-Secret header. These endpoints are not intended for direct client use.")
public class InternalOrderController {
    private final OrderService orderService;
    private final String internalSecret;

    public InternalOrderController(OrderService orderService, @Value("${app.internal-secret}") String internalSecret) {
        this.orderService = orderService;
        this.internalSecret = internalSecret;
    }

    @PostMapping("/{id}/payment-confirmed")
    @Operation(
            summary = "Mark order payment as confirmed",
            description = "Called by the payment service to notify that the payment for the specified order has been successfully processed. Transitions the order to a paid state."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment confirmed and order updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ApiResponse<OrderResponse> paymentConfirmed(
            @Parameter(description = "Unique identifier of the order", required = true) @PathVariable UUID id,
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret
    ) {
        ensureInternalSecret(secret);
        return ApiResponse.ok(orderService.markPayment(id, true));
    }

    @PostMapping("/{id}/payment-failed")
    @Operation(
            summary = "Mark order payment as failed",
            description = "Called by the payment service to notify that the payment attempt for the specified order has failed. The order will be moved to a failed/cancelled payment state."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment failure recorded and order updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ApiResponse<OrderResponse> paymentFailed(
            @Parameter(description = "Unique identifier of the order", required = true) @PathVariable UUID id,
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret
    ) {
        ensureInternalSecret(secret);
        return ApiResponse.ok(orderService.markPayment(id, false));
    }

    @PostMapping("/{id}/delivered")
    @Operation(
            summary = "Mark order as delivered (internal)",
            description = "Called by the delivery service when a courier completes the delivery. Transitions the order to DELIVERED (unless it is already in a terminal state). Requires the X-Internal-Secret header."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order marked as delivered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ApiResponse<OrderResponse> markDelivered(
            @Parameter(description = "Unique identifier of the order", required = true) @PathVariable UUID id,
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret
    ) {
        ensureInternalSecret(secret);
        return ApiResponse.ok(orderService.markDelivered(id));
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get internal order statistics for a time range",
            description = "Returns aggregate order statistics (counts, revenue totals, status breakdown) for the given date/time range. Used by the analytics service."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date/time format for 'from' or 'to' parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect")
    })
    public ApiResponse<InternalOrderStatsResponse> internalStats(
            @Parameter(description = "Start of the reporting window (ISO-8601 date-time)", required = true, example = "2026-01-01T00:00:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "End of the reporting window (ISO-8601 date-time)", required = true, example = "2026-06-30T23:59:59") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret
    ) {
        ensureInternalSecret(secret);
        return ApiResponse.ok(orderService.internalStats(from, to));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get an order by ID (internal)",
            description = "Retrieves the full details of any order by its ID without ownership checks. Intended for use by other services that need to look up order data."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ApiResponse<OrderResponse> getOrder(
            @Parameter(description = "Unique identifier of the order", required = true) @PathVariable UUID id,
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret
    ) {
        ensureInternalSecret(secret);
        return ApiResponse.ok(orderService.get(id, null, true));
    }

    @GetMapping("/usage/product/{productId}")
    @Operation(
            summary = "Check if a product is referenced by any order",
            description = "Returns whether the given productId appears in any order (used by product-service before deletion)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage check returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect")
    })
    public ApiResponse<ProductUsageCheckResponse> productUsage(
            @Parameter(description = "Product UUID to check", required = true) @PathVariable UUID productId,
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret
    ) {
        ensureInternalSecret(secret);
        long count = orderService.countOrdersByProduct(productId);
        return ApiResponse.ok(new ProductUsageCheckResponse(productId, count > 0, count));
    }

    private void ensureInternalSecret(String secret) {
        if (secret == null || !secret.equals(internalSecret)) {
            throw new OrderServiceException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Invalid internal secret");
        }
    }
}
