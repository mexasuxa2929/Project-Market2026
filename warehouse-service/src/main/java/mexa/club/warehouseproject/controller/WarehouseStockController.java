package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.warehouseproject.api.ApiResponse;
import mexa.club.warehouseproject.dto.CapacitySummaryResponse;
import mexa.club.warehouseproject.dto.LowStockAlertResponse;
import mexa.club.warehouseproject.dto.OrderReferenceRequest;
import mexa.club.warehouseproject.api.PagePayload;
import mexa.club.warehouseproject.dto.SalesOutRequest;
import mexa.club.warehouseproject.dto.SalesOutResponse;
import mexa.club.warehouseproject.dto.ProfitReportResponse;
import mexa.club.warehouseproject.dto.StockAdjustRequest;
import mexa.club.warehouseproject.dto.StockLotResponse;
import mexa.club.warehouseproject.dto.StockMinStockRequest;
import mexa.club.warehouseproject.dto.StockMovementResponse;
import mexa.club.warehouseproject.dto.StockTransferDecisionRequest;
import mexa.club.warehouseproject.dto.StockTransferRequest;
import mexa.club.warehouseproject.dto.StockTransferResponse;
import mexa.club.warehouseproject.dto.StockUpsertRequest;
import mexa.club.warehouseproject.dto.WarehouseStockLineResponse;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.service.ProfitReportService;
import mexa.club.warehouseproject.service.StockTransferService;
import mexa.club.warehouseproject.service.WarehouseStockService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Warehouse Stock", description = "Manage stock lines, adjustments, transfers, sales-out and movement history within a warehouse")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/warehouses/{warehouseId}/stock")
public class WarehouseStockController {

    private final WarehouseStockService warehouseStockService;
    private final ProfitReportService profitReportService;
    private final StockTransferService stockTransferService;

    public WarehouseStockController(
            WarehouseStockService warehouseStockService,
            ProfitReportService profitReportService,
            StockTransferService stockTransferService
    ) {
        this.warehouseStockService = warehouseStockService;
        this.profitReportService = profitReportService;
        this.stockTransferService = stockTransferService;
    }

    @Operation(summary = "List stock lines", description = "Returns a paginated list of all stock lines for the specified warehouse. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock lines returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<PagePayload<WarehouseStockLineResponse>>> list(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "Optional product name filter (partial match) resolved via product-service") @RequestParam(required = false) String search,
            @PageableDefault(size = 50, sort = "productId") Pageable pageable
    ) {
        var page = warehouseStockService.listStock(warehouseId, pageable, search);
        return ResponseEntity.ok(ApiResponse.ok(PagePayload.of(page)));
    }

    @Operation(summary = "Get capacity summary", description = "Returns the warehouse capacity in cubic meters, the total volume occupied by all stock lines (computed from each product's length/width/height × quantity), and the capacity usage percentage. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Capacity summary returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/capacity")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<CapacitySummaryResponse>> capacity(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseStockService.capacitySummary(warehouseId)));
    }

    @Operation(summary = "Get stock line by product variant", description = "Returns the stock line for a specific product variant in the warehouse. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock line returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or stock line not found")
    })
    @GetMapping("/lines/{productVariantId}")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<WarehouseStockLineResponse>> getLineByVariant(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the product variant", required = true) @PathVariable UUID productVariantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseStockService.getStockLineByProduct(warehouseId, productVariantId)));
    }

    @Operation(summary = "Get low stock alerts", description = "Returns a list of stock lines that are at or below their minimum quantity threshold. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Low stock alert list returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/alerts/low")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<List<LowStockAlertResponse>>> lowStockAlerts(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseStockService.listLowStockAlerts(warehouseId)));
    }

    @Operation(summary = "Upsert stock line", description = "Creates or updates the stock line for a product variant in the warehouse. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock line created or updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PutMapping("/lines")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<WarehouseStockLineResponse>> upsert(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Valid @RequestBody StockUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseStockService.upsertStock(warehouseId, request)));
    }

    @Operation(summary = "Update per-warehouse min stock override", description = "Sets or clears the min-stock threshold for a single product in this warehouse (null clears the override and falls back to the product default). Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Min stock override updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or stock line not found")
    })
    @PutMapping("/min-stock")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<WarehouseStockLineResponse>> updateMinStock(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Valid @RequestBody StockMinStockRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseStockService.updateMinStock(
                warehouseId, request.getProductId(), request.getMinStockOverride())));
    }

    @Operation(summary = "Adjust stock quantity", description = "Increases or decreases the quantity of a product variant in the warehouse by a given delta. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock adjusted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or insufficient stock for negative adjustment"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or stock line not found")
    })
    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<WarehouseStockLineResponse>> adjust(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Valid @RequestBody StockAdjustRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseStockService.adjustStock(warehouseId, request)));
    }

    @Operation(summary = "Transfer stock between warehouses", description = "Creates a pending transfer: deducts the quantity from this warehouse and requires the destination warehouse manager to confirm. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending transfer created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or insufficient stock"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Source or destination warehouse not found")
    })
    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> transfer(
            @Parameter(description = "UUID of the source warehouse", required = true) @PathVariable UUID warehouseId,
            @Valid @RequestBody StockTransferRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                stockTransferService.create(
                        warehouseId,
                        request.getToWarehouseId(),
                        request.getProductId(),
                        request.getQuantity()
                )
        ));
    }

    @Operation(summary = "List pending transfers", description = "Returns all transfers awaiting confirmation for this warehouse. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending transfers returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/transfers/pending")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<List<StockTransferResponse>>> pendingTransfers(
            @Parameter(description = "UUID of the destination warehouse", required = true) @PathVariable UUID warehouseId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockTransferService.listPending(warehouseId)));
    }

    @Operation(summary = "Confirm pending transfer", description = "Accepts the incoming stock transfer and adds the quantity to this warehouse's stock. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transfer confirmed and stock added"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or transfer not found")
    })
    @PostMapping("/transfers/{transferId}/confirm")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> confirmTransfer(
            @Parameter(description = "UUID of the destination warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the pending transfer", required = true) @PathVariable UUID transferId,
            @Valid @RequestBody(required = false) StockTransferDecisionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockTransferService.confirm(
                warehouseId, transferId, request != null ? request.getNote() : null)));
    }

    @Operation(summary = "Reject pending transfer", description = "Rejects the incoming stock transfer and restores the quantity to the source warehouse. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transfer rejected and source stock restored"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or transfer not found")
    })
    @PostMapping("/transfers/{transferId}/reject")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> rejectTransfer(
            @Parameter(description = "UUID of the destination warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the pending transfer", required = true) @PathVariable UUID transferId,
            @Valid @RequestBody(required = false) StockTransferDecisionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockTransferService.reject(
                warehouseId, transferId, request != null ? request.getNote() : null)));
    }

    @Operation(summary = "Delete stock line", description = "Removes the stock line for a product variant from the warehouse. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Stock line deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or stock line not found")
    })
    @DeleteMapping("/lines/{productVariantId}")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<Void> deleteLine(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the product variant whose stock line should be deleted", required = true) @PathVariable UUID productVariantId
    ) {
        warehouseStockService.deleteStockLine(warehouseId, productVariantId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Record sales-out", description = "Deducts sold quantities from stock and records a SALES_OUT movement. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sales-out recorded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or insufficient stock"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or stock line not found")
    })
    @PostMapping("/sales-out")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<SalesOutResponse>> salesOut(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Valid @RequestBody SalesOutRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseStockService.salesOut(warehouseId, request)));
    }

    @Operation(summary = "Reverse sales-out", description = "Reverses a previously recorded sales-out by restoring the deducted quantities. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sales-out reversed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or sales-out not reversible"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or original sales-out record not found")
    })
    @PostMapping("/sales-out/reverse")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<SalesOutResponse>> reverseSalesOut(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Valid @RequestBody OrderReferenceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseStockService.reverseSalesOut(warehouseId, request.getOrderId())));
    }

    @Operation(summary = "Get stock movement history", description = "Returns a paginated and filterable list of stock movements for the warehouse. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Movement history returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<PagePayload<StockMovementResponse>>> history(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "Filter by product variant UUID") @RequestParam(required = false) UUID productVariantId,
            @Parameter(description = "Filter by movement type (e.g. PURCHASE, SALES_OUT, ADJUSTMENT, TRANSFER_IN, TRANSFER_OUT)") @RequestParam(required = false) MovementType movementType,
            @Parameter(description = "Filter movements created at or after this timestamp (ISO-8601)") @RequestParam(required = false) LocalDateTime dateFrom,
            @Parameter(description = "Filter movements created at or before this timestamp (ISO-8601)") @RequestParam(required = false) LocalDateTime dateTo,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable
    ) {
        var page = warehouseStockService.stockHistory(warehouseId, productVariantId, movementType, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagePayload.of(page)));
    }

    @Operation(summary = "List stock lots", description = "Returns the FIFO lots for a warehouse, optionally filtered by product. Each lot carries its own unit cost from the originating purchase. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lots returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/lots")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<List<StockLotResponse>>> lots(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "Optional filter by product UUID") @RequestParam(required = false) UUID productId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseStockService.listLots(warehouseId, productId)));
    }

    @Operation(summary = "Profit report", description = "Computes revenue (current sale price × sold qty) minus FIFO lot COGS for all products sold in the warehouse. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profit report returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/report/profit")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<ProfitReportResponse>> profitReport(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "Filter movements created at or after this timestamp (ISO-8601)") @RequestParam(required = false) LocalDateTime dateFrom,
            @Parameter(description = "Filter movements created at or before this timestamp (ISO-8601)") @RequestParam(required = false) LocalDateTime dateTo
    ) {
        return ResponseEntity.ok(ApiResponse.ok(profitReportService.profitReport(warehouseId, dateFrom, dateTo)));
    }
}
