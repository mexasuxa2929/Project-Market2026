package mexa.club.paymentservice.payme;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mexa.club.paymentservice.payme.audit.PaymeAuditService;
import mexa.club.paymentservice.payme.dto.JsonRpcRequest;
import mexa.club.paymentservice.payme.dto.JsonRpcResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment - Payme Callback", description = "JSON-RPC webhook endpoint for Payme payment gateway callbacks. All methods are dispatched through a single POST endpoint.")
@RestController
@RequestMapping("/payment/payme")
@RequiredArgsConstructor
public class PaymeController {

    private final PaymeAuthFilter authFilter;
    private final PaymeService paymeService;
    private final PaymeAuditService auditService;

    @Operation(
        summary = "Payme JSON-RPC callback",
        description = "Single entry point for all Payme payment gateway callbacks. The method to execute is determined by the 'method' field in the JSON-RPC request body. " +
            "Supported methods: CheckPerformTransaction, CreateTransaction, PerformTransaction, CancelTransaction, CheckTransaction, GetStatement. " +
            "Requires Payme Basic Authentication in the Authorization header."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "JSON-RPC response returned (check 'error' field in body for Payme-level errors)"),
        @ApiResponse(responseCode = "401", description = "Invalid or missing Payme Authorization credentials")
    })
    @PostMapping("/callback")
    public JsonRpcResponse callback(
            @RequestBody JsonRpcRequest req,
            @Parameter(description = "Payme Basic Auth credentials (Base64-encoded Cashier:token)", required = false)
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!authFilter.isValid(authorization)) {
            JsonRpcResponse resp = JsonRpcResponse.fail(req.getId(), PaymeErrors.auth());
            auditService.log(req, resp);
            return resp;
        }

        if (req.getMethod() == null || req.getMethod().isBlank()) {
            JsonRpcResponse resp = JsonRpcResponse.fail(req.getId(), PaymeErrors.invalidRequest());
            auditService.log(req, resp);
            return resp;
        }

        JsonRpcResponse resp =
                switch (req.getMethod()) {
                    case "CheckPerformTransaction" -> paymeService.checkPerform(req);
                    case "CreateTransaction" -> paymeService.createTransaction(req);
                    case "PerformTransaction" -> paymeService.performTransaction(req);
                    case "CancelTransaction" -> paymeService.cancelTransaction(req);
                    case "CheckTransaction" -> paymeService.checkTransaction(req);
                    case "GetStatement" -> paymeService.getStatement(req);
                    default -> JsonRpcResponse.fail(req.getId(), PaymeErrors.methodNotFound());
                };
        auditService.log(req, resp);
        return resp;
    }
}
