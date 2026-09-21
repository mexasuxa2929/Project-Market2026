package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.warehouseproject.service.WarehouseStockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Tag(name = "Internal — Stock Cleanup", description = "Internal service-to-service endpoint for removing stock lines when products are hard-deleted. Protected by a shared API key header (X-Internal-Api-Key), not by JWT.")
@RestController
@RequestMapping("/internal/stock-lines")
public class InternalStockCleanupController {

    private final WarehouseStockService warehouseStockService;
    private final String internalApiKey;

    public InternalStockCleanupController(
            WarehouseStockService warehouseStockService,
            @Value("${app.internal-api.key:change-me-internal}") String internalApiKey
    ) {
        this.warehouseStockService = warehouseStockService;
        this.internalApiKey = internalApiKey;
    }

    /**
     * Deletes all stock lines for a given productVariantId.
     * Called by product-service when a product is hard-deleted (rare — prefer soft-delete).
     */
    @Operation(
            summary = "Delete all stock lines for a product variant",
            description = "Removes every stock line associated with the specified product variant UUID across all warehouses. Called by product-service when a product variant is permanently (hard) deleted. Prefer soft-delete to avoid triggering this endpoint. Authentication is performed via the X-Internal-Api-Key header rather than a JWT Bearer token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock lines deleted; response body contains the count of deleted rows"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid X-Internal-Api-Key header"),
            @ApiResponse(responseCode = "404", description = "No stock lines found for the given product variant")
    })
    @DeleteMapping("/variants/{productVariantId}")
    public ResponseEntity<Map<String, Object>> deleteAllByVariant(
            @Parameter(description = "UUID of the product variant whose stock lines should be deleted", required = true) @PathVariable UUID productVariantId,
            @Parameter(description = "Internal API key for service-to-service authentication", required = true) @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedKey
    ) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid internal API key");
        }
        long deleted = warehouseStockService.deleteAllByProductId(productVariantId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deletedCount", deleted
        ));
    }
}
