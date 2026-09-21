package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.shopservice.service.ProductReviewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Tag(name = "Internal — Reviews", description = "Internal service-to-service endpoint for deleting reviews when a product is hard-deleted. Protected by a shared API key header (X-Internal-Api-Key), not by JWT.")
@RestController
@RequestMapping("/api/shops/internal/products")
public class InternalReviewController {

    private final ProductReviewService reviewService;
    private final String internalApiKey;

    public InternalReviewController(
            ProductReviewService reviewService,
            @Value("${app.internal-api.key:change-me-internal}") String internalApiKey
    ) {
        this.reviewService = reviewService;
        this.internalApiKey = internalApiKey;
    }

    @Operation(
            summary = "Delete all reviews for a product",
            description = "Removes every review associated with the specified product UUID. Called by product-service when a product is hard-deleted. Authentication is performed via the X-Internal-Api-Key header rather than a JWT Bearer token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid X-Internal-Api-Key header")
    })
    @DeleteMapping("/{productId}/reviews")
    public ResponseEntity<Void> deleteByProduct(
            @Parameter(description = "UUID of the product whose reviews should be deleted", required = true)
            @PathVariable UUID productId,
            @Parameter(description = "Internal API key for service-to-service authentication", required = true)
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedKey
    ) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid internal API key");
        }
        reviewService.deleteByProductId(productId);
        return ResponseEntity.ok().build();
    }
}
