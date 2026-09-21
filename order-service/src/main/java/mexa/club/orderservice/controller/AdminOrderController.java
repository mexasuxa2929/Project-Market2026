package mexa.club.orderservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.orderservice.dto.AdminNoteRequest;
import mexa.club.orderservice.dto.ApiResponse;
import mexa.club.orderservice.dto.OrderResponse;
import mexa.club.orderservice.dto.OrderStatsResponse;
import mexa.club.orderservice.dto.OrderStatusHistoryResponse;
import mexa.club.orderservice.dto.PagePayload;
import mexa.club.orderservice.dto.RefundRequest;
import mexa.club.orderservice.dto.UpdateOrderStatusRequest;
import mexa.club.orderservice.entity.OrderStatus;
import mexa.club.orderservice.entity.PaymentStatus;
import mexa.club.orderservice.exception.OrderServiceException;
import mexa.club.orderservice.security.JwtUserPrincipal;
import mexa.club.orderservice.service.OrderService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAuthority('ORDER_MANAGE')")
@Tag(name = "Admin - Orders", description = "Administrative endpoints for managing all orders in the system. Requires the ORDER_MANAGE authority.")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {
    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(
            summary = "List all orders",
            description = "Returns a paginated list of all orders. Supports filtering by status, paymentStatus, userId, orderNumber, and date range."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of orders returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have the ORDER_MANAGE authority")
    })
    public ApiResponse<PagePayload<OrderResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) UUID warehouseId
    ) {
        boolean hasFilters = status != null || paymentStatus != null || userId != null
                || orderNumber != null || dateFrom != null || dateTo != null || warehouseId != null;
        if (hasFilters) {
            return ApiResponse.ok(orderService.searchOrders(
                    status, paymentStatus, userId, orderNumber, dateFrom, dateTo, warehouseId, page, size));
        }
        return ApiResponse.ok(orderService.allOrders(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get any order by ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order details returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have the ORDER_MANAGE authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ApiResponse<OrderResponse> detail(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ApiResponse.ok(orderService.get(id, userId(authentication), true));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(orderService.updateStatus(id, userId(authentication), request));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get order statistics")
    public ApiResponse<OrderStatsResponse> stats() {
        return ApiResponse.ok(orderService.stats());
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get order status history")
    public ApiResponse<List<OrderStatusHistoryResponse>> history(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ApiResponse.ok(orderService.getOrderHistory(id));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund an order", description = "Refunds a DELIVERED order: changes status to REFUNDED, reverses stock, cancels discount.")
    public ApiResponse<OrderResponse> refund(
            @PathVariable UUID id,
            @RequestBody(required = false) RefundRequest request,
            Authentication authentication
    ) {
        String reason = request != null ? request.reason() : null;
        return ApiResponse.ok(orderService.refundOrder(id, userId(authentication), reason));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an order", description = "Deletes a CANCELLED or REFUNDED order from the system.")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        orderService.deleteOrder(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export")
    @Operation(summary = "Export orders as CSV")
    public ResponseEntity<Resource> export(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
    ) {
        List<OrderResponse> orders = orderService.exportOrders(status, paymentStatus, dateFrom, dateTo);
        String csv = buildCsv(orders);
        byte[] bytes = csv.getBytes();
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(bytes.length)
                .body(resource);
    }

    @PutMapping("/{id}/admin-note")
    @Operation(summary = "Update admin note on an order")
    public ApiResponse<OrderResponse> updateAdminNote(
            @PathVariable UUID id,
            @Valid @RequestBody AdminNoteRequest request
    ) {
        return ApiResponse.ok(orderService.updateAdminNote(id, request));
    }

    private UUID userId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtUserPrincipal jwtUserPrincipal) {
            return jwtUserPrincipal.userId();
        }
        throw new OrderServiceException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid authentication principal");
    }

    private String buildCsv(List<OrderResponse> orders) {
        String header = "OrderNumber,UserId,Status,PaymentStatus,Subtotal,DeliveryFee,TotalAmount,Currency,CreatedAt\n";
        String rows = orders.stream().map(o -> String.format("%s,%s,%s,%s,%.2f,%.2f,%.2f,%s,%s",
                csvEscape(o.orderNumber()),
                o.userId(),
                o.status(),
                o.paymentStatus(),
                o.subtotal(),
                o.deliveryFee(),
                o.totalAmount(),
                o.currency(),
                o.createdAt()
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
