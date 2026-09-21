package mexa.club.authservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class AuthExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> badCredentials(BadCredentialsException ex) {
        log.warn("Login failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("INVALID_CREDENTIALS", "Invalid username or password"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> illegalState(IllegalStateException ex) {
        if ("Not authenticated".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorBody("UNAUTHORIZED", "Authentication required"));
        }
        log.error("Illegal state", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("INTERNAL_ERROR", ex.getMessage() != null ? ex.getMessage() : "Operation failed"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a + "; " + b,
                        HashMap::new));
        Map<String, Object> err = new HashMap<>();
        err.put("code", "VALIDATION_ERROR");
        err.put("message", "Validation failed");
        err.put("fieldErrors", fields);
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", err);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName() != null ? ex.getName() : "parameter";
        Class<?> required = ex.getRequiredType();
        String hint = required != null && required.equals(java.util.UUID.class)
                ? "UUID formatida bo‘lishi kerak (masalan: 550e8400-e29b-41d4-a716-446655440000)."
                : "Kutilgan tur bilan mos kelmaydi.";
        log.warn("Type mismatch for {}: {}", name, ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("BAD_REQUEST", "Noto‘g‘ri " + name + ": " + hint));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(OtpNotFoundException.class)
    public ResponseEntity<Map<String, Object>> otpNotFound(OtpNotFoundException ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.startsWith("Email already registered")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(errorBody("EMAIL_IN_USE", msg));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody("OTP_NOT_FOUND", msg != null ? msg : "Not found"));
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<Map<String, Object>> otpExpired(OtpExpiredException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("OTP_EXPIRED", ex.getMessage()));
    }

    @ExceptionHandler(OtpTooManyAttemptsException.class)
    public ResponseEntity<Map<String, Object>> otpTooManyAttempts(OtpTooManyAttemptsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(errorBody("OTP_TOO_MANY_ATTEMPTS", ex.getMessage()));
    }

    @ExceptionHandler(OtpInvalidException.class)
    public ResponseEntity<Map<String, Object>> otpInvalid(OtpInvalidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("OTP_INVALID", ex.getMessage()));
    }

    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<Map<String, Object>> invalidGoogleToken(InvalidGoogleTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("INVALID_GOOGLE_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(RefreshTokenDeviceMismatchException.class)
    public ResponseEntity<Map<String, Object>> deviceMismatch(RefreshTokenDeviceMismatchException ex) {
        log.warn("Refresh token device mismatch: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("DEVICE_MISMATCH", "Refresh token is bound to a different device"));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> roleNotFound(RoleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody("ROLE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RoleAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> roleAlreadyExists(RoleAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody("ROLE_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(SystemRoleException.class)
    public ResponseEntity<Map<String, Object>> systemRole(SystemRoleException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody("SYSTEM_ROLE_PROTECTED", ex.getMessage()));
    }

    @ExceptionHandler(ResendRateLimitedException.class)
    public ResponseEntity<Map<String, Object>> resendRateLimited(ResendRateLimitedException ex) {
        Map<String, Object> err = new HashMap<>();
        err.put("code", "RESEND_RATE_LIMITED");
        err.put("message", ex.getMessage());
        err.put("waitSeconds", ex.getWaitSeconds());

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", err);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> fallback(Exception ex) {
        log.error("Unhandled auth-service error", ex);
        try {
            java.io.FileWriter fw = new java.io.FileWriter("C:\\Users\\Mehrob\\AppData\\Local\\Temp\\opencode\\auth_error.log", true);
            java.io.PrintWriter pw = new java.io.PrintWriter(fw);
            ex.printStackTrace(pw);
            pw.close();
            fw.close();
        } catch (Exception ignored) {}
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private static Map<String, Object> errorBody(String code, String message) {
        Map<String, Object> err = Map.of("code", code, "message", message);
        return Map.of("success", false, "error", err);
    }
}
