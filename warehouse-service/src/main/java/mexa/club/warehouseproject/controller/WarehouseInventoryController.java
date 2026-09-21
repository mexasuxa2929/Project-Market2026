package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.warehouseproject.api.ApiResponse;
import mexa.club.warehouseproject.api.PagePayload;
import mexa.club.warehouseproject.dto.InventoryCountRequest;
import mexa.club.warehouseproject.dto.InventoryCreateRequest;
import mexa.club.warehouseproject.dto.InventorySessionResponse;
import mexa.club.warehouseproject.service.InventoryService;
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

@Tag(name = "Warehouse Inventory", description = "Physical inventory count sessions — start, record counts, complete or cancel")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/warehouses/{warehouseId}/inventory")
public class WarehouseInventoryController {

    private final InventoryService inventoryService;

    public WarehouseInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "Start inventory session", description = "Opens a new inventory count session for the warehouse. An optional request body may specify which products to count; omitting it includes all stock lines. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory session started successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<InventorySessionResponse>> start(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @RequestBody(required = false) InventoryCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.start(warehouseId, request)));
    }

    @Operation(summary = "List inventory sessions", description = "Returns a paginated list of inventory sessions for the warehouse. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory sessions returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<PagePayload<InventorySessionResponse>>> list(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(PagePayload.of(inventoryService.list(warehouseId, pageable))));
    }

    @Operation(summary = "Get inventory session", description = "Returns a single inventory session with its item list and counted quantities. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory session returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or session not found")
    })
    @GetMapping("/{sessionId}")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<InventorySessionResponse>> get(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the inventory session", required = true) @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.get(warehouseId, sessionId)));
    }

    @Operation(summary = "Record item count", description = "Records the physically counted quantity for a specific item in the inventory session. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item count recorded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse, session, or item not found")
    })
    @PutMapping("/{sessionId}/items/{itemId}")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<InventorySessionResponse>> count(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the inventory session", required = true) @PathVariable UUID sessionId,
            @Parameter(description = "UUID of the inventory item to update", required = true) @PathVariable UUID itemId,
            @Valid @RequestBody InventoryCountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.countItem(warehouseId, sessionId, itemId, request)));
    }

    @Operation(summary = "Complete inventory session", description = "Finalizes the inventory session. Stock quantities are reconciled with the counted values and adjustments are applied. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory session completed and stock reconciled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or session not found")
    })
    @PostMapping("/{sessionId}/complete")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<InventorySessionResponse>> complete(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the inventory session to complete", required = true) @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.complete(warehouseId, sessionId)));
    }

    @Operation(summary = "Cancel inventory session", description = "Cancels an in-progress inventory session without applying any stock changes. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory session cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or session not found")
    })
    @PostMapping("/{sessionId}/cancel")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<InventorySessionResponse>> cancel(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the inventory session to cancel", required = true) @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.cancel(warehouseId, sessionId)));
    }
}
