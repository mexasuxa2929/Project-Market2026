package mexa.club.paymentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.paymentservice.dto.ApiResponse;
import mexa.club.paymentservice.dto.RefundRequest;
import mexa.club.paymentservice.dto.RefundResponse;
import mexa.club.paymentservice.exception.PaymentException;
import mexa.club.paymentservice.service.RefundService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal - Payments", description = "Service-to-service endpoints for the payment domain. All requests must include the X-Internal-Secret header. These endpoints are not intended for direct client use.")
@RestController
@RequestMapping(value = "/internal/payments", produces = MediaType.APPLICATION_JSON_VALUE)
public class InternalPaymentController {
    private final RefundService refundService;
    private final String internalSecret;

    public InternalPaymentController(RefundService refundService, @Value("${app.internal-secret}") String internalSecret) {
        this.refundService = refundService;
        this.internalSecret = internalSecret;
    }

    @PostMapping(value = "/refund", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Refund a paid order via Payme",
            description = "Called by the order-service to return the money for a paid order. Finds the captured Payme transaction, performs CancelTransaction against the Payme merchant API and marks the transaction as refunded. Idempotent when the transaction is already refunded."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund completed (or already refunded)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid refund request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or incorrect"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment or transaction not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "No captured payment to refund"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Payme rejected the refund or is unavailable")
    })
    public ApiResponse<RefundResponse> refund(
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true) @RequestHeader("X-Internal-Secret") String secret,
            @RequestBody RefundRequest request
    ) {
        ensureInternalSecret(secret);
        return ApiResponse.ok(refundService.refund(
                request != null ? request.orderId() : null,
                request != null ? request.reason() : null));
    }

    private void ensureInternalSecret(String secret) {
        if (secret == null || !java.security.MessageDigest.isEqual(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                internalSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new PaymentException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Invalid internal secret");
        }
    }
}
