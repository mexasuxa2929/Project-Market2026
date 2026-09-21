package mexa.club.discountservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.discountservice.dto.ApiResponse;
import mexa.club.discountservice.dto.CreatePromotionRequest;
import mexa.club.discountservice.dto.PageResponse;
import mexa.club.discountservice.dto.PromotionResponse;
import mexa.club.discountservice.dto.PromotionStatsResponse;
import mexa.club.discountservice.dto.PromotionUsageResponse;
import mexa.club.discountservice.dto.UpdatePromotionRequest;
import mexa.club.discountservice.entity.PromotionType;
import mexa.club.discountservice.service.PromotionAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/promotions")
@PreAuthorize("hasAuthority('DISCOUNT_MANAGE')")
@Tag(name = "Admin - Promotions", description = "Administrative endpoints for creating and managing promotions and discount campaigns. Requires the DISCOUNT_MANAGE authority.")
@SecurityRequirement(name = "bearerAuth")
public class AdminPromotionController {

    private final PromotionAdminService promotionAdminService;

    public AdminPromotionController(PromotionAdminService promotionAdminService) {
        this.promotionAdminService = promotionAdminService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new promotion",
            description = "Creates a new discount promotion. The promotion type (COUPON, PROMO_CODE, AUTOMATIC, etc.) and applicable rules must be specified in the request body."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Promotion created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation failure"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have the DISCOUNT_MANAGE authority")
    })
    public ResponseEntity<ApiResponse<PromotionResponse>> create(@Valid @RequestBody CreatePromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(promotionAdminService.create(request)));
    }

    @GetMapping
    @Operation(
            summary = "List promotions",
            description = "Returns a filtered, paginated list of promotions. Results can be filtered by active status, promotion type, and a free-text search term."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of promotions returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have the DISCOUNT_MANAGE authority")
    })
    public ResponseEntity<ApiResponse<PageResponse<PromotionResponse>>> list(
            @Parameter(description = "Filter by active status. Omit to return all promotions regardless of status.") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter by promotion type (e.g., COUPON, PROMO_CODE, AUTOMATIC).") @RequestParam(required = false) PromotionType type,
            @Parameter(description = "Free-text search term to filter by promotion name or code.") @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of promotions per page", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(promotionAdminService.list(active, type, search, page, size)));
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get promotion statistics",
            description = "Returns aggregate statistics across all promotions, including total usage counts, total discount amounts granted, and active vs. inactive promotion counts."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Promotion statistics returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have the DISCOUNT_MANAGE authority")
    })
    public ResponseEntity<ApiResponse<PromotionStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(promotionAdminService.stats()));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get promotion by ID",
            description = "Returns the full details of a single promotion including its rules, validity period, and current status."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Promotion details returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have the DISCOUNT_MANAGE authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Promotion not found")
    })
    public ResponseEntity<ApiResponse<PromotionResponse>> get(
            @Parameter(description = "Unique identifier of the promotion", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(promotionAdminService.get(id)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a promotion",
            description = "Updates the details of an existing promotion such as its name, discount value, validity dates, or usage limits. Only the fields provided in the request body are updated."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Promotion updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have the DISCOUNT_MANAGE authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Promotion not found")
    })
    public ResponseEntity<ApiResponse<PromotionResponse>> update(
            @Parameter(description = "Unique identifier of the promotion to update", required = true) @PathVariable UUID id,
            @RequestBody UpdatePromotionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(promotionAdminService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Soft-delete a promotion",
            description = "Marks a promotion as deleted without removing it from the database. The promotion will no longer be visible or applicable to customers."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Promotion deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have the DISCOUNT_MANAGE authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Promotion not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Unique identifier of the promotion to delete", required = true) @PathVariable UUID id
    ) {
        promotionAdminService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{id}/usages")
    @Operation(
            summary = "Get promotion usage history",
            description = "Returns a paginated list of individual usage records for the specified promotion, showing which customers used it and when."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage history returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have the DISCOUNT_MANAGE authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Promotion not found")
    })
    public ResponseEntity<ApiResponse<PageResponse<PromotionUsageResponse>>> usages(
            @Parameter(description = "Unique identifier of the promotion", required = true) @PathVariable UUID id,
            @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of usage records per page", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(promotionAdminService.usages(id, page, size)));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(
            summary = "Deactivate a promotion",
            description = "Immediately deactivates the specified promotion so it can no longer be applied by customers. The promotion record is retained for reporting purposes."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Promotion deactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have the DISCOUNT_MANAGE authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Promotion not found")
    })
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @Parameter(description = "Unique identifier of the promotion to deactivate", required = true) @PathVariable UUID id
    ) {
        promotionAdminService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
