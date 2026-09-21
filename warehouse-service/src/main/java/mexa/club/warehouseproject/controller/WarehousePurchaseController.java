package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.warehouseproject.api.ApiResponse;
import mexa.club.warehouseproject.api.PagePayload;
import mexa.club.warehouseproject.dto.PurchaseCreateRequest;
import mexa.club.warehouseproject.dto.PurchaseItemUpdateRequest;
import mexa.club.warehouseproject.dto.PurchaseResponse;
import mexa.club.warehouseproject.dto.PurchaseUpdateRequest;
import mexa.club.warehouseproject.service.PurchaseService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Warehouse Purchases", description = "Record and manage purchase orders for a warehouse")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/warehouses/{warehouseId}/purchases")
public class WarehousePurchaseController {

    private final PurchaseService purchaseService;

    public WarehousePurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @Operation(summary = "List purchases", description = "Returns a paginated list of purchase orders for the warehouse, sorted by purchase date descending by default. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Purchase list returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<PagePayload<PurchaseResponse>>> list(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @PageableDefault(size = 20, sort = "purchaseDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var page = purchaseService.listPurchases(warehouseId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagePayload.of(page)));
    }

    @Operation(summary = "Get purchase by ID", description = "Returns a single purchase order with its items. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Purchase returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or purchase not found")
    })
    @GetMapping("/{purchaseId}")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> get(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the purchase order", required = true) @PathVariable UUID purchaseId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseService.getPurchase(warehouseId, purchaseId)));
    }

    @Operation(
            summary = "Create purchase",
            description = """
                    Records a new purchase order and increases stock accordingly.

                    Correct request example:
                    {
                      "invoiceNumber": "INV-2024-001",
                      "items": [
                        {
                          "productVariantId": "174187b7-d1f2-44cf-8a2d-3a9d760ce5eb",
                          "quantity": 50,
                          "unitPrice": 40000.00
                        }
                      ]
                    }

                    Workflow:
                    Step 1: GET /api/products/{id}/variants -> get productVariantId
                    Step 2: POST /api/warehouses/{warehouseId}/purchases -> use productVariantId

                    Requires STOCK_MANAGE authority.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Purchase created and stock increased"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> create(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Valid @RequestBody PurchaseCreateRequest request
    ) {
        PurchaseResponse created = purchaseService.createPurchase(warehouseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @Operation(summary = "Update purchase item", description = "Updates the quantity or unit price of a specific item within a purchase order. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Purchase item updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse, purchase, or item not found")
    })
    @PutMapping("/{purchaseId}/items/{itemId}")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> updateItem(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the purchase order", required = true) @PathVariable UUID purchaseId,
            @Parameter(description = "UUID of the purchase item to update", required = true) @PathVariable UUID itemId,
            @Valid @RequestBody PurchaseItemUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                purchaseService.updatePurchaseItem(warehouseId, purchaseId, itemId, request)
        ));
    }

    @Operation(summary = "Update purchase header", description = "Updates header-level fields of a purchase order (e.g. invoice number, purchase date). Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Purchase header updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or purchase not found")
    })
    @PutMapping("/{purchaseId}")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> updateHeader(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the purchase order", required = true) @PathVariable UUID purchaseId,
            @RequestBody PurchaseUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                purchaseService.updatePurchaseHeader(warehouseId, purchaseId, request)
        ));
    }

    @Operation(summary = "Delete purchase item", description = "Removes a single item from a purchase order and reverses the stock addition. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Purchase item deleted and stock reversed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse, purchase, or item not found")
    })
    @DeleteMapping("/{purchaseId}/items/{itemId}")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<ApiResponse<PurchaseResponse>> deleteItem(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the purchase order", required = true) @PathVariable UUID purchaseId,
            @Parameter(description = "UUID of the purchase item to delete", required = true) @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                purchaseService.deletePurchaseItem(warehouseId, purchaseId, itemId)
        ));
    }

    @Operation(summary = "Delete purchase order", description = "Deletes an entire purchase order and reverses all stock additions associated with it. Requires STOCK_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Purchase order deleted and stock reversed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or purchase not found")
    })
    @DeleteMapping("/{purchaseId}")
    @PreAuthorize("hasAuthority('STOCK_MANAGE')")
    public ResponseEntity<Void> deletePurchase(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the purchase order to delete", required = true) @PathVariable UUID purchaseId
    ) {
        purchaseService.deletePurchase(warehouseId, purchaseId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Download purchase PDF", description = "Generates and downloads a PDF document for the specified purchase order. Requires STOCK_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF generated and returned as binary file"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — STOCK_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or purchase not found")
    })
    @GetMapping("/{purchaseId}/pdf")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    public ResponseEntity<byte[]> purchasePdf(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "UUID of the purchase order", required = true) @PathVariable UUID purchaseId
    ) {
        return purchaseService.purchasePdf(warehouseId, purchaseId);
    }
}
