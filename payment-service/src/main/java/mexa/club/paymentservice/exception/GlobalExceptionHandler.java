package mexa.club.paymentservice.exception;

import java.util.Map;
import mexa.club.paymentservice.payme.PaymeErrors;
import mexa.club.paymentservice.payme.dto.JsonRpcResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public Object fallback(Exception ex, jakarta.servlet.http.HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri != null && uri.contains("/payment/payme")) {
            return JsonRpcResponse.fail(null, PaymeErrors.internal());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "internal_error"));
    }
}
