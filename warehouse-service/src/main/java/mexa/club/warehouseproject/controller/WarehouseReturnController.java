package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.warehouseproject.api.ApiResponse;
import mexa.club.warehouseproject.api.PagePayload;
import mexa.club.warehouseproject.dto.ApproveRejectRequest;
import mexa.club.warehouseproject.dto.StockReturnCreateRequest;
import mexa.club.warehouseproject.dto.StockReturnResponse;
import mexa.club.warehouseproject.service.StockReturnService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Warehouse Returns", description = "Manage customer stock return requests within a warehouse")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/warehouses/{warehouseId}/returns")
public class WarehouseReturnController {

    private final StockReturnService stockReturnService;

    public WarehouseReturnController(StockReturnService stockReturnService) {
        this.stockReturnService = stockReturnService;
    }

    @Operation(summary = "Create stock return", description = "Submits a new stock return request for the warehouse. The return must be approved before stock is restored. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock return created and pending approval"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<StockReturnResponse>> create(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Valid @RequestBody StockReturnCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockReturnService.create(warehouseId, request)));
    }

    @Operation(summary = "List stock returns", description = "Returns a paginated list of all stock return requests for the warehouse. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Return list returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<PagePayload<StockReturnResponse>>> list(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(PagePayload.of(stockReturnService.list(warehouseId, pageable))));
    }

    @Operation(summary = "Get stock return by ID", description = "Returns a single stock return request. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock return returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or return not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<StockReturnResponse>> get(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the stock return", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockReturnService.get(warehouseId, id)));
    }

    @Operation(summary = "Approve stock return", description = "Approves a pending stock return and restores the returned quantity to the warehouse stock. An optional note can be included. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock return approved and stock restored"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Return is not in a state that allows approval"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or return not found")
    })
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<StockReturnResponse>> approve(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the stock return to approve", required = true) @PathVariable UUID id,
            @RequestBody(required = false) ApproveRejectRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockReturnService.approve(warehouseId, id, request != null ? request.getNote() : null)));
    }

    @Operation(summary = "Reject stock return", description = "Rejects a pending stock return. No stock changes occur. An optional note explaining the rejection can be included. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock return rejected"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Return is not in a state that allows rejection"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or return not found")
    })
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<StockReturnResponse>> reject(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the stock return to reject", required = true) @PathVariable UUID id,
            @RequestBody(required = false) ApproveRejectRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockReturnService.reject(warehouseId, id, request != null ? request.getNote() : null)));
    }
}
