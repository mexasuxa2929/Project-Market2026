package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.shopservice.client.payload.ApiResponse;
import mexa.club.shopservice.dto.CreateReviewRequest;
import mexa.club.shopservice.dto.MyReviewResponse;
import mexa.club.shopservice.exception.ProductNotFoundException;
import mexa.club.shopservice.security.JwtUserPrincipal;
import mexa.club.shopservice.security.RequestTokenProvider;
import mexa.club.shopservice.security.ShopUsers;
import mexa.club.shopservice.service.ProductReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "My Reviews", description = "Product review management for the authenticated customer")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/shops/me/reviews")
@PreAuthorize("isAuthenticated()")
public class MeReviewController {

    private final ProductReviewService reviewService;
    private final RequestTokenProvider tokenProvider;

    public MeReviewController(ProductReviewService reviewService,
                               RequestTokenProvider tokenProvider) {
        this.reviewService = reviewService;
        this.tokenProvider = tokenProvider;
    }

    /** Mahsulot uchun mening reviewimni olish (yo'q bo'lsa 404) */
    @Operation(
        summary = "Get my review for a product",
        description = "Returns the authenticated user's review for the specified product. Returns 404 if the user has not yet reviewed this product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found for this product")
    })
    @GetMapping("/{productId}")
    public ApiResponse<MyReviewResponse> getMyReview(
            Authentication authentication,
            @Parameter(description = "UUID of the product to retrieve the review for", required = true)
            @PathVariable UUID productId
    ) {
        UUID userId = ShopUsers.requireUserId(authentication);
        return reviewService.getMyReview(userId, productId)
                .map(ApiResponse::ok)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    /** Review yaratish yoki yangilash (faqat yetkazib berilgan buyurtmadan keyin) */
    @Operation(
        summary = "Create or update my review",
        description = "Creates a new review or updates an existing one for the specified product. The user must have a delivered order containing this product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review created or updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid review data or user has not purchased this product"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping
    public ApiResponse<MyReviewResponse> upsert(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest body
    ) {
        UUID   userId     = ShopUsers.requireUserId(authentication);
        String username   = resolveUsername(authentication);
        String authHeader = tokenProvider.resolveAuthorizationHeader();
        return ApiResponse.ok(reviewService.createOrUpdate(userId, username, authHeader, body));
    }

    private static String resolveUsername(Authentication auth) {
        if (auth.getPrincipal() instanceof JwtUserPrincipal jwt) return jwt.username();
        return auth.getName();
    }
}
