package mexa.club.discountservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.discountservice.dto.ApiResponse;
import mexa.club.discountservice.dto.ApplyDiscountResponse;
import mexa.club.discountservice.dto.InternalDiscountApplyRequest;
import mexa.club.discountservice.dto.InternalDiscountConfirmRequest;
import mexa.club.discountservice.dto.InternalDiscountStatsResponse;
import mexa.club.discountservice.security.InternalSecretVerifier;
import mexa.club.discountservice.service.DiscountService;
import mexa.club.discountservice.service.InternalDiscountAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/internal/discounts")
@Tag(name = "Internal - Discounts", description = "Service-to-service endpoints for discount operations. All requests must include the X-Internal-Secret header. Primarily consumed by the order service.")
public class InternalDiscountController {

    private final DiscountService discountService;
    private final InternalSecretVerifier internalSecretVerifier;
    private final InternalDiscountAnalyticsService internalDiscountAnalyticsService;

    public InternalDiscountController(
            DiscountService discountService,
            InternalSecretVerifier internalSecretVerifier,
            InternalDiscountAnalyticsService internalDiscountAnalyticsService
    ) {
        this.discountService = discountService;
        this.internalSecretVerifier = internalSecretVerifier;
        this.internalDiscountAnalyticsService = internalDiscountAnalyticsService;
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get discount statistics for a time range",
            description = "Returns aggregate discount usage statistics (total discounts granted, total discount amount, unique users, top promotions) for the specified date/time range. Used by the analytics service."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Discount statistics returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date/time format for 'from' or 'to' parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect")
    })
    public ResponseEntity<ApiResponse<InternalDiscountStatsResponse>> stats(
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret,
            @Parameter(description = "Start of the reporting window (ISO-8601 date-time)", required = true, example = "2026-01-01T00:00:00") @RequestParam @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME,
                    fallbackPatterns = {
                            "M/d/yy, h:mm a",
                            "M/d/yy, h:mm a",
                            "MM/d/yy, h:mm a",
                            "MM/d/yy, h:mm a",
                            "M/d/yyyy, h:mm a",
                            "M/d/yyyy, h:mm a",
                            "yyyy-MM-dd HH:mm:ss",
                    }
            ) LocalDateTime from,
            @Parameter(description = "End of the reporting window (ISO-8601 date-time)", required = true, example = "2026-06-30T23:59:59") @RequestParam @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME,
                    fallbackPatterns = {
                            "M/d/yy, h:mm a",
                            "M/d/yy, h:mm a",
                            "MM/d/yy, h:mm a",
                            "MM/d/yy, h:mm a",
                            "M/d/yyyy, h:mm a",
                            "M/d/yyyy, h:mm a",
                            "yyyy-MM-dd HH:mm:ss",
                    }
            ) LocalDateTime to
    ) {
        internalSecretVerifier.verify(secret);
        LocalDateTime now = LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.ok(internalDiscountAnalyticsService.stats(from, to, now)));
    }

    @PostMapping("/apply")
    @Operation(
            summary = "Apply and record a discount",
            description = "Applies a discount to an order and records the usage in a reserved (pending) state. Called by the order service during order creation. The discount usage is not finalised until the confirm endpoint is called."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Discount applied and reserved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or the promotion is not applicable"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect")
    })
    public ResponseEntity<ApiResponse<ApplyDiscountResponse>> apply(
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret,
            @Valid @RequestBody InternalDiscountApplyRequest request
    ) {
        internalSecretVerifier.verify(secret);
        return ResponseEntity.ok(ApiResponse.ok(discountService.internalApplyAndRecord(request)));
    }

    @PostMapping("/confirm")
    @Operation(
            summary = "Confirm a discount usage",
            description = "Finalises a previously reserved discount usage after the order has been successfully confirmed and payment captured. This increments the promotion's usage counter permanently."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Discount usage confirmed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or no matching reserved usage found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect")
    })
    public ResponseEntity<ApiResponse<Void>> confirm(
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret,
            @Valid @RequestBody InternalDiscountConfirmRequest request
    ) {
        internalSecretVerifier.verify(secret);
        discountService.confirmUsage(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/cancel/{orderId}")
    @Operation(
            summary = "Cancel a discount usage for an order",
            description = "Releases the reserved discount usage associated with the given order ID. Called when an order is cancelled before payment, freeing the promotion slot for future use."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Discount usage cancelled and released successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No reserved discount usage found for the given order ID")
    })
    public ResponseEntity<ApiResponse<Void>> cancel(
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret,
            @Parameter(description = "Unique identifier of the order whose discount usage should be cancelled", required = true) @PathVariable UUID orderId
    ) {
        internalSecretVerifier.verify(secret);
        discountService.cancelUsage(orderId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
