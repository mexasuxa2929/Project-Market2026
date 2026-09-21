package mexa.club.productservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.productservice.api.ApiResponse;
import mexa.club.productservice.dto.ProductPriceRequest;
import mexa.club.productservice.dto.ProductPriceResponse;
import mexa.club.productservice.service.ProductPriceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/prices")
@Tag(name = "Product Prices", description = "Manage price history entries for a product. Each price record tracks the retail price effective from a specific date.")
@SecurityRequirement(name = "bearerAuth")
public class ProductPriceController {

    private final ProductPriceService productPriceService;

    public ProductPriceController(ProductPriceService productPriceService) {
        this.productPriceService = productPriceService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(
        summary = "List prices for a product",
        description = "Returns all price history records associated with the given product, ordered by effective date."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Price list returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given productId")
    })
    public ResponseEntity<ApiResponse<List<ProductPriceResponse>>> list(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.ok(productPriceService.listForProduct(productId)));
    }

    @GetMapping("/{priceId}")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(
        summary = "Get a specific price record",
        description = "Returns a single price record by its UUID, scoped to the given product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Price record returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Price record not found for the given productId and priceId")
    })
    public ResponseEntity<ApiResponse<ProductPriceResponse>> get(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId,
            @Parameter(description = "UUID of the price record", required = true) @PathVariable UUID priceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productPriceService.getPrice(productId, priceId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Add a price record to a product",
        description = "Creates a new price history entry for the specified product. The new price becomes effective from the date specified in the request body."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Price record created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given productId")
    })
    public ResponseEntity<ApiResponse<ProductPriceResponse>> add(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Price data including amount and effective date", required = true)
            @Valid @RequestBody ProductPriceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(productPriceService.addPrice(productId, request)));
    }

    @PutMapping("/{priceId}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Update a price record",
        description = "Replaces the fields of an existing price history entry for the specified product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Price record updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Price record not found for the given productId and priceId")
    })
    public ResponseEntity<ApiResponse<ProductPriceResponse>> update(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId,
            @Parameter(description = "UUID of the price record to update", required = true) @PathVariable UUID priceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated price data", required = true)
            @Valid @RequestBody ProductPriceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productPriceService.updatePrice(productId, priceId, request)));
    }

    @DeleteMapping("/{priceId}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Delete a price record",
        description = "Removes a price history entry from the specified product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Price record deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Price record not found for the given productId and priceId")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID productId,
            @Parameter(description = "UUID of the price record to delete", required = true) @PathVariable UUID priceId
    ) {
        productPriceService.deletePrice(productId, priceId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
