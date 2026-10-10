package mexa.club.paymentservice.exception;

import java.util.Map;
import mexa.club.paymentservice.payme.PaymeErrors;
import mexa.club.paymentservice.payme.dto.JsonRpcResponse;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePayment(PaymentException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "success", false,
                "code", ex.getCode(),
                "message", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object badJson(HttpMessageNotReadableException ex, jakarta.servlet.http.HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri != null && uri.contains("/payment/payme")) {
            return JsonRpcResponse.fail(null, PaymeErrors.parse());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invalid_json"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> badBody(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "validation_failed"));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public Object handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
    }


    // O-31: IllegalArgumentException (controller/yordamchi kodidan) -> 400, 500 emas
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal argument", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "code", "BAD_REQUEST",
                "message", ex.getMessage() != null ? ex.getMessage() : "Invalid request"
        ));
    }

    @ExceptionHandler(Exception.class)
    public Object fallback(Exception ex, jakarta.servlet.http.HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri != null && uri.contains("/payment/payme")) {
            return JsonRpcResponse.fail(null, PaymeErrors.internal());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "internal_error"));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<java.util.Map<String, Object>> handleResponseStatus(org.springframework.web.server.ResponseStatusException ex) {
        java.util.Map<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("code", String.valueOf(ex.getStatusCode().value()));
        error.put("message", ex.getReason() != null ? ex.getReason() : ex.getLocalizedMessage());
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("success", false);
        body.put("error", error);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}
