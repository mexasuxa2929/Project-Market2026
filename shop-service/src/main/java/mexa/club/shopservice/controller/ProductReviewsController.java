package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.shopservice.client.payload.ApiResponse;
import mexa.club.shopservice.dto.RatingStatsResponse;
import mexa.club.shopservice.dto.RecommendedProductResponse;
import mexa.club.shopservice.dto.ReviewPageResponse;
import mexa.club.shopservice.service.ProductReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Product Reviews (Public)", description = "Public endpoints for reading product reviews, ratings and recommendations")
@RestController
@RequestMapping("/api/shops/products")
public class ProductReviewsController {

    private final ProductReviewService reviewService;

    public ProductReviewsController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Mahsulot reviewlari ro'yxati (ommaviy) */
    @Operation(
        summary = "List reviews for a product",
        description = "Returns a paginated list of approved reviews for the specified product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{productId}/reviews")
    public ApiResponse<ReviewPageResponse> list(
            @Parameter(description = "UUID of the product", required = true)
            @PathVariable UUID productId,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Number of reviews per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(reviewService.listPublic(productId, page, size));
    }

    /** Mahsulot reyting statistikasi: o'rtacha ball, soni, taqsimot */
    @Operation(
        summary = "Get rating statistics for a product",
        description = "Returns the average rating score, total review count, and distribution of ratings (1-5 stars) for the given product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rating statistics returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{productId}/rating")
    public ApiResponse<RatingStatsResponse> rating(
            @Parameter(description = "UUID of the product", required = true)
            @PathVariable UUID productId) {
        return ApiResponse.ok(reviewService.getRatingStats(productId));
    }

    /** Rekomendatsiya qilingan mahsulotlar (eng yuqori reytingli) */
    @Operation(
        summary = "Get recommended products",
        description = "Returns the top-rated products across the catalog, ordered by average rating descending."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommended products returned successfully")
    })
    @GetMapping("/recommended")
    public ApiResponse<List<RecommendedProductResponse>> recommended(
            @Parameter(description = "Maximum number of recommended products to return")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Only products with an active discount (home screen)")
            @RequestParam(defaultValue = "false") boolean discountOnly
    ) {
        return ApiResponse.ok(reviewService.getRecommended(limit, discountOnly));
    }

    /** Bir nechta mahsulot uchun reytingni batch qilib olish (product-service uchun). */
    @Operation(
        summary = "Get ratings for multiple products",
        description = "Returns average rating and review count for each of the given product IDs."
    )
    @GetMapping("/rating-batch")
    public ApiResponse<java.util.Map<UUID, double[]>> ratingBatch(
            @Parameter(description = "Product IDs (comma-separated)")
            @RequestParam("ids") List<UUID> ids
    ) {
        return ApiResponse.ok(reviewService.getRatingBatch(ids));
    }
}
