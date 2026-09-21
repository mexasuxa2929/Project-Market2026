package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.shopservice.client.payload.ApiResponse;
import mexa.club.shopservice.client.payload.PagePayload;
import mexa.club.shopservice.dto.WishlistAddRequest;
import mexa.club.shopservice.dto.WishlistEntryResponse;
import mexa.club.shopservice.security.ShopUsers;
import mexa.club.shopservice.service.WishlistService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "My Wishlist", description = "Wishlist management for the authenticated customer")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/shops/me/wishlist")
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class MeWishlistController {

    private final WishlistService wishlistService;

    public MeWishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /**
     * Wishlist ro'yxati (paginatsiyali).
     * GET /api/shops/me/wishlist?page=0&size=20
     */
    @Operation(
        summary = "List my wishlist",
        description = "Returns a paginated list of products saved to the authenticated user's wishlist."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wishlist retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ApiResponse<PagePayload<WishlistEntryResponse>> list(
            Authentication authentication,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Number of wishlist items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = ShopUsers.requireUserId(authentication);
        Page<WishlistEntryResponse> result = wishlistService.listPage(userId, page, size);
        return ApiResponse.ok(new PagePayload<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        ));
    }

    @Operation(
        summary = "Add a product to my wishlist",
        description = "Adds the specified product to the authenticated user's wishlist. If the product is already in the wishlist, the request is idempotent."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product added to wishlist"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping
    public ApiResponse<WishlistEntryResponse> add(
            Authentication authentication,
            @Valid @RequestBody WishlistAddRequest body
    ) {
        UUID userId = ShopUsers.requireUserId(authentication);
        return ApiResponse.ok(wishlistService.add(userId, body));
    }

    @Operation(
        summary = "Check if a product is in my wishlist",
        description = "Returns whether the specified product is in the authenticated user's wishlist."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/{productId}/check")
    public ApiResponse<Boolean> check(
            Authentication authentication,
            @Parameter(description = "UUID of the product to check", required = true)
            @PathVariable UUID productId) {
        UUID userId = ShopUsers.requireUserId(authentication);
        return ApiResponse.ok(wishlistService.isFavorited(userId, productId));
    }

    @Operation(
        summary = "Remove a product from my wishlist",
        description = "Removes the specified product from the authenticated user's wishlist."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product removed from wishlist"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found in wishlist")
    })
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(
            Authentication authentication,
            @Parameter(description = "UUID of the product to remove from wishlist", required = true)
            @PathVariable UUID productId) {
        UUID userId = ShopUsers.requireUserId(authentication);
        wishlistService.remove(userId, productId);
        return ApiResponse.okVoid();
    }
}
