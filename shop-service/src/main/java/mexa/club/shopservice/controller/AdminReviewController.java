package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.shopservice.client.payload.ApiResponse;
import mexa.club.shopservice.dto.AdminReviewResponse;
import mexa.club.shopservice.service.ProductReviewService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin - Reviews", description = "Admin endpoints for managing and moderating product reviews")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/shops/admin/reviews")
@PreAuthorize("hasAnyAuthority('PRODUCT_VIEW','PRODUCT_MANAGE')")
public class AdminReviewController {

    private final ProductReviewService reviewService;

    public AdminReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Barcha sharhlar (opsional: productId bo'yicha filter).
     * GET /api/shops/admin/reviews?productId=...&page=0&size=20
     */
    @Operation(
        summary = "List all reviews",
        description = "Returns a paginated list of all product reviews across the platform. Optionally filter by a specific product UUID. Requires PRODUCT_VIEW or PRODUCT_MANAGE authority."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review list retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @GetMapping
    public ApiResponse<PageDto<AdminReviewResponse>> list(
            @Parameter(description = "Filter reviews by product UUID (optional)")
            @RequestParam(required = false) UUID productId,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Number of reviews per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AdminReviewResponse> p = reviewService.listAllAdmin(productId, page, size);
        return ApiResponse.ok(new PageDto<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages()
        ));
    }

    /**
     * Sharhni o'chirish.
     * DELETE /api/shops/admin/reviews/{reviewId}
     */
    @Operation(
        summary = "Delete a review",
        description = "Permanently removes the specified review. Requires PRODUCT_MANAGE authority."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Review deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions — PRODUCT_MANAGE authority required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found")
    })
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the review to delete", required = true)
            @PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    /** Pagination wrapper */
    public record PageDto<T>(
            java.util.List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
