package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.shopservice.client.payload.ApiResponse;
import mexa.club.shopservice.dto.order.OrderPagePayload;
import mexa.club.shopservice.dto.order.OrderResponsePayload;
import mexa.club.shopservice.dto.order.ShopCreateOrderRequest;
import mexa.club.shopservice.exception.UnauthorizedShopException;
import mexa.club.shopservice.security.RequestTokenProvider;
import mexa.club.shopservice.security.ShopUsers;
import mexa.club.shopservice.service.OrderProxyService;
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

@Tag(name = "My Orders", description = "Order management endpoints for the authenticated customer")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/shops/me/orders")
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class MeOrderController {

    private final OrderProxyService orderProxyService;
    private final RequestTokenProvider requestTokenProvider;

    public MeOrderController(OrderProxyService orderProxyService, RequestTokenProvider requestTokenProvider) {
        this.orderProxyService = orderProxyService;
        this.requestTokenProvider = requestTokenProvider;
    }

    @Operation(
        summary = "List my orders",
        description = "Returns a paginated list of orders placed by the authenticated user, ordered by creation date descending."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order list retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ApiResponse<OrderPagePayload<OrderResponsePayload>> list(
            Authentication authentication,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of orders per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        ShopUsers.requireUserId(authentication);
        String auth = requireBearer();
        return ApiResponse.ok(orderProxyService.listMyOrders(auth, page, size));
    }

    @Operation(
        summary = "Get order details",
        description = "Returns the full details of a specific order belonging to the authenticated user."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Order belongs to another user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ApiResponse<OrderResponsePayload> detail(
            Authentication authentication,
            @Parameter(description = "UUID of the order", required = true)
            @PathVariable UUID id) {
        ShopUsers.requireUserId(authentication);
        String auth = requireBearer();
        return ApiResponse.ok(orderProxyService.getOrder(auth, id));
    }

    @Operation(
        summary = "Create a new order",
        description = "Places a new order for the authenticated user. The cart contents, delivery address, and payment method must be provided in the request body."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid order request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping
    public ApiResponse<OrderResponsePayload> create(
            Authentication authentication,
            @Valid @RequestBody ShopCreateOrderRequest body
    ) {
        ShopUsers.requireUserId(authentication);
        String auth = requireBearer();
        return ApiResponse.ok(orderProxyService.createOrder(auth, body));
    }

    @Operation(
        summary = "Cancel an order",
        description = "Cancels the specified order if it is still in a cancellable state. An optional cancellation reason can be provided in the request body as {\"reason\": \"...\"}."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order cannot be cancelled in its current state"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponsePayload> cancel(
            Authentication authentication,
            @Parameter(description = "UUID of the order to cancel", required = true)
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        ShopUsers.requireUserId(authentication);
        String auth = requireBearer();
        String reason = body != null ? body.get("reason") : null;
        return ApiResponse.ok(orderProxyService.cancelOrder(auth, id, reason));
    }

    private String requireBearer() {
        String header = requestTokenProvider.resolveAuthorizationHeader();
        if (header == null || header.isBlank()) {
            throw new UnauthorizedShopException("Authorization header required");
        }
        return header;
    }
}
