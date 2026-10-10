package mexa.club.notificationservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<Map<String, Object>> handle(NotificationException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "success", false,
                "code", ex.getCode(),
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "code", "NOT_FOUND",
                "message", "Endpoint not found"
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
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "code", "INTERNAL_ERROR",
                "message", "An unexpected error occurred"
        ));
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
