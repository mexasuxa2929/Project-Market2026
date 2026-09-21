package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.warehouseproject.dto.InternalWarehouseStatsResponse;
import mexa.club.warehouseproject.service.WarehouseStockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Tag(name = "Internal — Warehouse Stats", description = "Internal service-to-service endpoint for aggregated low-stock statistics. Protected by a shared API key header (X-Internal-Api-Key), not by JWT.")
@RestController
@RequestMapping("/internal/warehouse")
public class InternalWarehouseStatsController {

    private final WarehouseStockService warehouseStockService;
    private final String internalApiKey;

    public InternalWarehouseStatsController(
            WarehouseStockService warehouseStockService,
            @Value("${app.internal-api.key:change-me-internal}") String internalApiKey
    ) {
        this.warehouseStockService = warehouseStockService;
        this.internalApiKey = internalApiKey;
    }

    @Operation(
            summary = "Get aggregated low-stock statistics",
            description = "Returns warehouse-level statistics about products whose stock quantity is at or below the specified threshold. Intended for internal service-to-service communication only. Authentication is performed via the X-Internal-Api-Key header rather than a JWT Bearer token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Low-stock statistics returned successfully"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid X-Internal-Api-Key header")
    })
    @GetMapping("/stats")
    public ResponseEntity<InternalWarehouseStatsResponse> stats(
            @Parameter(description = "Low-stock threshold — products with quantity at or below this value are included in the response (default: 10)") @RequestParam(defaultValue = "10") int threshold,
            @Parameter(description = "Internal API key for service-to-service authentication", required = true) @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedKey
    ) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid internal API key");
        }
        return ResponseEntity.ok(warehouseStockService.internalAggregateLowStock(threshold));
    }
}
