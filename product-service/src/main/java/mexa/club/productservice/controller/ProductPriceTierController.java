package mexa.club.productservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.productservice.api.ApiResponse;
import mexa.club.productservice.dto.ProductPriceTierBulkRequest;
import mexa.club.productservice.dto.ProductPriceTierRequest;
import mexa.club.productservice.dto.ProductPriceTierResponse;
import mexa.club.productservice.service.ProductPriceTierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/price-tiers")
@Tag(name = "Product Price Tiers", description = "Manage wholesale price tiers (quantity-based pricing) for a product. Each tier defines a minimum quantity threshold and the corresponding unit price.")
@SecurityRequirement(name = "bearerAuth")
public class ProductPriceTierController {

    private final ProductPriceTierService tierService;

    public ProductPriceTierController(ProductPriceTierService tierService) {
        this.tierService = tierService;
    }

    /** Mahsulotning barcha narx bosqichlarini olish */
    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(
        summary = "List price tiers for a product",
        description = "Returns all wholesale price tiers defined for the given product, ordered by minimum quantity threshold."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Price tier list returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given productId")
    })
    public ResponseEntity<ApiResponse<List<ProductPriceTierResponse>>> list(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tierService.listForProduct(productId)));
    }

    /** Yangi narx bosqichi qo'shish */
    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Add a price tier to a product",
        description = "Creates a new wholesale price tier for the specified product. The tier defines a minimum order quantity and the discounted unit price that applies from that quantity onward."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Price tier created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given productId")
    })
    public ResponseEntity<ApiResponse<ProductPriceTierResponse>> add(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Price tier data including minimum quantity and unit price", required = true)
            @Valid @RequestBody ProductPriceTierRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(tierService.add(productId, request)));
    }

    /** Narx bosqichini yangilash */
    @PutMapping("/{tierId}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Update a price tier",
        description = "Replaces the fields of an existing price tier for the given product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Price tier updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Price tier or product not found")
    })
    public ResponseEntity<ApiResponse<ProductPriceTierResponse>> update(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId,
            @Parameter(description = "UUID of the price tier to update", required = true) @PathVariable UUID tierId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated price tier data", required = true)
            @Valid @RequestBody ProductPriceTierRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tierService.update(productId, tierId, request)));
    }

    /** Narx bosqichini o'chirish */
    @DeleteMapping("/{tierId}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Delete a price tier",
        description = "Removes a wholesale price tier from the specified product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Price tier deleted successfully — no content returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Price tier or product not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId,
            @Parameter(description = "UUID of the price tier to delete", required = true) @PathVariable UUID tierId
    ) {
        tierService.delete(productId, tierId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Barcha tier larni bir yo'la almashtirish (mavjudlari o'chiriladi).
     * Frontend mahsulot yaratgandan so'ng barcha local tier larni yuborish uchun ishlatadi.
     * PUT /api/products/{productId}/price-tiers/bulk
     */
    @PutMapping("/bulk")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Bulk-replace all price tiers",
        description = "Atomically replaces all existing price tiers of a product with the provided list. All current tiers are deleted first; the supplied tiers are then inserted. Useful for the frontend to sync locally-built tier tables after product creation."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All price tiers replaced and new list returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the tier list"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given productId")
    })
    public ResponseEntity<ApiResponse<List<ProductPriceTierResponse>>> bulkReplace(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Complete list of price tiers to set for the product", required = true)
            @Valid @RequestBody ProductPriceTierBulkRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tierService.bulkReplace(productId, request)));
    }

    /**
     * Berilgan miqdor uchun narxni hisoblaydi.
     * GET /api/products/{productId}/price-tiers/resolve?qty=25
     * Response: { found, qty, price, total, tierIndex, discountPercent }
     */
    @GetMapping("/resolve")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(
        summary = "Resolve the applicable price for a quantity",
        description = "Given a desired order quantity, finds the matching wholesale price tier and returns the applicable unit price, order total in UZS, and the discount percentage relative to the base (first-tier) price. Returns {found: false} if no tiers are configured."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Price resolved successfully. 'found' field indicates whether a matching tier exists."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given productId")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolve(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId,
            @Parameter(description = "Order quantity to resolve the price for", required = true) @RequestParam int qty
    ) {
        Optional<BigDecimal> price = tierService.resolvePrice(productId, qty);
        if (price.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "found", false,
                    "qty",   qty
            )));
        }

        BigDecimal unitPrice = price.get();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));

        // Base narx (1-tier narxi)
        List<ProductPriceTierResponse> allTiers = tierService.listForProduct(productId);
        BigDecimal basePrice = allTiers.isEmpty() ? null : allTiers.get(0).getPrice();

        Double discountPct = null;
        if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0
                && unitPrice.compareTo(basePrice) < 0) {
            discountPct = basePrice.subtract(unitPrice)
                    .divide(basePrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("found",           true);
        result.put("qty",             qty);
        result.put("price",           unitPrice);
        result.put("total",           total);
        result.put("currency",        "UZS");
        if (discountPct != null) result.put("discountPercent", discountPct);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** Validation xatolarini ushlaydigan handler */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("VALIDATION_ERROR", ex.getMessage()));
    }
}
