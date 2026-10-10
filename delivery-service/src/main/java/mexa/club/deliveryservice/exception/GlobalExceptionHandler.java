package mexa.club.deliveryservice.exception;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(DeliveryException.class)
    public ResponseEntity<Map<String, Object>> handle(DeliveryException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "success", false,
                "code", ex.getCode(),
                "message", ex.getMessage()
        ));
    }

    // O-31: validatsiya xatlari 500 emas — 400 + field xabarlari
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        Map<String, String> fields = new java.util.HashMap<>();
        for (org.springframework.validation.FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "code", "VALIDATION_ERROR",
                "fields", fields
        ));
    }

    // O-31: IllegalArgumentException -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal argument", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "code", "BAD_REQUEST",
                "message", ex.getMessage() != null ? ex.getMessage() : "Invalid request"
        ));
    }

    // O-31: qayta ishlanmagan xato — global fallback (500 generik)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "code", "INTERNAL_ERROR",
                "message", "An unexpected error occurred"
        ));
    }

}
