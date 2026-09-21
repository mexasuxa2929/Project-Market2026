package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.warehouseproject.dto.StockBatchByWarehouseResponse;
import mexa.club.warehouseproject.dto.StockBatchRequest;
import mexa.club.warehouseproject.dto.StockBatchResponse;
import mexa.club.warehouseproject.service.StockBatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Batch stock queries used primarily by product-service to enrich /api/products/{id}/full responses.
 */
@Tag(name = "Stock Batch Queries", description = "Batch endpoints for querying aggregated or per-warehouse stock levels across multiple product variants — primarily consumed by product-service")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/stock")
public class StockBatchController {

    private final StockBatchService stockBatchService;

    public StockBatchController(StockBatchService stockBatchService) {
        this.stockBatchService = stockBatchService;
    }

    /**
     * Returns aggregated stock (SUM across all warehouses) per variant.
     * Used by product-service to build the shop-facing /full response.
     */
    @Operation(summary = "Get total stock for multiple variants", description = "Accepts a list of product variant UUIDs and returns the total stock quantity summed across all warehouses for each variant. Used by product-service to build shop-facing product responses. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aggregated stock totals returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required")
    })
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<List<StockBatchResponse>> getBatchStock(
            @Valid @RequestBody StockBatchRequest request
    ) {
        return ResponseEntity.ok(stockBatchService.getTotalStockBatch(request.getProductIds()));
    }

    /**
     * Returns per-warehouse breakdown of stock for each variant.
     * Used for detailed inventory analysis.
     */
    @Operation(summary = "Get per-warehouse stock for multiple variants", description = "Accepts a list of product variant UUIDs and returns a per-warehouse stock breakdown for each variant. Useful for detailed inventory analysis and warehouse-level availability checks. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Per-warehouse stock breakdown returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required")
    })
    @PostMapping("/batch/by-warehouse")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<List<StockBatchByWarehouseResponse>> getBatchStockByWarehouse(
            @Valid @RequestBody StockBatchRequest request
    ) {
        return ResponseEntity.ok(stockBatchService.getStockByWarehouseBatch(request.getProductIds()));
    }
}
