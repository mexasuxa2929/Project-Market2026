package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.shopservice.client.payload.ApiResponse;
import mexa.club.shopservice.dto.CartLineResponse;
import mexa.club.shopservice.dto.CartQuantityRequest;
import mexa.club.shopservice.dto.CartResponse;
import mexa.club.shopservice.security.ShopUsers;
import mexa.club.shopservice.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "My Cart", description = "Shopping cart management for the authenticated customer")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/shops/me/cart")
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class MeCartController {

    private final CartService cartService;

    public MeCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(
        summary = "Get current cart",
        description = "Returns the full shopping cart of the authenticated user, including all line items and the total price."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ApiResponse<CartResponse> get(
            Authentication authentication,
            @Parameter(description = "Til kodi (uz|ru) — berilsa mahsulot nomlari shu tilda lokalizatsiyalanadi")
            @org.springframework.web.bind.annotation.RequestParam(required = false) String lang) {
        UUID userId = ShopUsers.requireUserId(authentication);
        return ApiResponse.ok(cartService.getCart(userId, lang));
    }

    @Operation(
        summary = "Add or update a cart item",
        description = "Sets the quantity of the specified product in the cart. If the item does not exist it is created; if quantity is zero the item is removed."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or quantity"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/items/{productId}")
    public ApiResponse<CartLineResponse> putLine(
            Authentication authentication,
            @Parameter(description = "UUID of the product to add or update", required = true)
            @PathVariable UUID productId,
            @Valid @RequestBody CartQuantityRequest body,
            @Parameter(description = "Til kodi (uz|ru) — berilsa javobdagi mahsulot nomi shu tilda bo'ladi")
            @org.springframework.web.bind.annotation.RequestParam(required = false) String lang
    ) {
        UUID userId = ShopUsers.requireUserId(authentication);
        return ApiResponse.ok(cartService.upsertLine(userId, productId, body.quantity(), lang));
    }

    @Operation(
        summary = "Remove a cart item",
        description = "Removes the specified product line from the authenticated user's cart."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item removed from cart"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found in cart")
    })
    @DeleteMapping("/items/{productId}")
    public ApiResponse<Void> deleteLine(
            Authentication authentication,
            @Parameter(description = "UUID of the product to remove", required = true)
            @PathVariable UUID productId) {
        UUID userId = ShopUsers.requireUserId(authentication);
        cartService.removeLine(userId, productId);
        return ApiResponse.okVoid();
    }

    @Operation(
        summary = "Clear the entire cart",
        description = "Removes all items from the authenticated user's shopping cart."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @DeleteMapping
    public ApiResponse<Void> clear(Authentication authentication) {
        UUID userId = ShopUsers.requireUserId(authentication);
        cartService.clear(userId);
        return ApiResponse.okVoid();
    }
}
