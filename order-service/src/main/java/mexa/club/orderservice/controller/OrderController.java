package mexa.club.orderservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.orderservice.dto.ApiResponse;
import mexa.club.orderservice.dto.CreateOrderRequest;
import mexa.club.orderservice.dto.OrderResponse;
import mexa.club.orderservice.dto.PagePayload;
import mexa.club.orderservice.exception.OrderServiceException;
import mexa.club.orderservice.security.JwtUserPrincipal;
import mexa.club.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
@Tag(name = "Customer Orders", description = "Endpoints for authenticated customers to create, view, and cancel their own orders.")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(
            summary = "Place a new order",
            description = "Creates a new order for the currently authenticated user. The request must include at least one order item with a valid product ID and quantity."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation failure"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid")
    })
    public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        return ApiResponse.ok(orderService.create(userId(authentication), request));
    }

    @GetMapping
    @Operation(
            summary = "List my orders",
            description = "Returns a paginated list of orders placed by the currently authenticated user, sorted by creation date descending."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of orders returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid")
    })
    public ApiResponse<PagePayload<OrderResponse>> listMine(
            @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of orders per page", example = "20") @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ApiResponse.ok(orderService.myOrders(userId(authentication), page, size));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get order details",
            description = "Returns full details of a specific order. The order must belong to the currently authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order details returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Order does not belong to the authenticated user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ApiResponse<OrderResponse> detail(
            @Parameter(description = "Unique identifier of the order", required = true) @PathVariable UUID id,
            Authentication authentication
    ) {
        return ApiResponse.ok(orderService.get(id, userId(authentication), false));
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel an order",
            description = "Requests cancellation of an existing order. Only orders in a cancellable state (e.g., PENDING) can be cancelled. An optional cancellation reason may be provided in the request body."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order is not in a cancellable state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Order does not belong to the authenticated user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ApiResponse<OrderResponse> cancel(
            @Parameter(description = "Unique identifier of the order to cancel", required = true) @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication
    ) {
        String reason = body != null ? body.get("reason") : null;
        return ApiResponse.ok(orderService.cancel(id, userId(authentication), reason));
    }

    private UUID userId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtUserPrincipal jwtUserPrincipal) {
            return jwtUserPrincipal.userId();
        }
        throw new OrderServiceException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid authentication principal");
    }
}
