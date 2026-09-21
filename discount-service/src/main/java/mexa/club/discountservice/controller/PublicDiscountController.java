package mexa.club.discountservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.discountservice.dto.ApiResponse;
import mexa.club.discountservice.dto.ApplyDiscountRequest;
import mexa.club.discountservice.dto.ApplyDiscountResponse;
import mexa.club.discountservice.dto.PublicPromotionResponse;
import mexa.club.discountservice.exception.DiscountException;
import mexa.club.discountservice.security.JwtUserPrincipal;
import mexa.club.discountservice.service.DiscountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/discounts")
@Tag(name = "Discounts", description = "Public endpoints for authenticated customers to browse active promotions and preview discount calculations on their cart.")
@SecurityRequirement(name = "bearerAuth")
public class PublicDiscountController {

    private final DiscountService discountService;

    public PublicDiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping("/apply")
    @Operation(
            summary = "Preview discount application",
            description = "Calculates and previews the discount that would be applied to the given cart or order. This is a read-only operation — it does not consume the promotion. The caller must be authenticated via JWT."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Discount preview calculated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or the promo code is not applicable"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or the user ID cannot be extracted")
    })
    public ResponseEntity<ApiResponse<ApplyDiscountResponse>> apply(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody ApplyDiscountRequest request
    ) {
        UUID userId = principal != null ? principal.userId() : null;
        if (userId == null) {
            throw new DiscountException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "JWT userId is missing");
        }
        ApplyDiscountResponse result = discountService.applyPreview(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/active")
    @Operation(
            summary = "List active promotions",
            description = "Returns all currently active and publicly visible promotions. The caller must be authenticated via JWT."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of active promotions returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or the user ID cannot be extracted")
    })
    public ResponseEntity<ApiResponse<List<PublicPromotionResponse>>> active(
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null || principal.userId() == null) {
            throw new DiscountException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "JWT userId is missing");
        }
        return ResponseEntity.ok(ApiResponse.ok(discountService.listActivePublic()));
    }
}
