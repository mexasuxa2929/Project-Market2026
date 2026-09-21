package mexa.club.shopservice.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import mexa.club.shopservice.client.payload.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "code", "NOT_FOUND",
                        "message", ex.getMessage()
                ));
    }

    /**
     * @Valid validatsiya xatosi: default resolver 400 qo'yadi, lekin javob
     * mijozgacha bo'sh 403 bo'lib yetib borardi — explicit handler to'g'ri
     * 400 JSON qaytaradi: {"code":"VALIDATION_ERROR","fields":{...}}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new java.util.LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                fields.put(e.getField(), e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid"));
        log.debug("Validation failed: {}", fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, null, new ApiResponse.ErrorPayload("VALIDATION_ERROR", "Validation failed: " + fields)));
    }

    /**
     * Downstream servis (order/product-service) 4xx qaytarganda RestClient
     * HttpClientErrorException otadi. Avval bu hech qayerga ilinmay, mijozga
     * bo'sh 403 ko'rinishida chiqardi — endi asl status + asl xabar uzatiladi.
     * Masalan: qayta cancel -> 400 INVALID_STATUS "Only PENDING order can be cancelled".
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ApiResponse<Object>> handleDownstreamClientError(HttpClientErrorException ex) {
        String code = "UPSTREAM_ERROR";
        String message = ex.getMessage();
        try {
            Map<?, ?> body = MAPPER.readValue(ex.getResponseBodyAsString(), Map.class);
            if (body.get("code") != null) {
                code = String.valueOf(body.get("code"));
            }
            if (body.get("message") != null) {
                message = String.valueOf(body.get("message"));
            }
        } catch (Exception parseEx) {
            log.debug("Downstream error body parse failed: {}", parseEx.getMessage());
        }
        log.warn("Downstream 4xx propagated as {}: {} - {}", ex.getStatusCode(), code, message);
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiResponse<>(false, null, new ApiResponse.ErrorPayload(code, message)));
    }

    /** Downstream servis 5xx qaytarganda -> 502, asl xabar bilan. */
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ApiResponse<Object>> handleDownstreamServerError(HttpServerErrorException ex) {
        String code = "UPSTREAM_ERROR";
        String message = ex.getMessage();
        try {
            Map<?, ?> body = MAPPER.readValue(ex.getResponseBodyAsString(), Map.class);
            if (body.get("code") != null) {
                code = String.valueOf(body.get("code"));
            }
            if (body.get("message") != null) {
                message = String.valueOf(body.get("message"));
            }
        } catch (Exception parseEx) {
            log.debug("Downstream error body parse failed: {}", parseEx.getMessage());
        }
        log.warn("Downstream 5xx: {} - {}", code, message);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiResponse<>(false, null, new ApiResponse.ErrorPayload(code, message)));
    }

    /** Downstream servisga ulanib bo'lmaganda (connection refused/timeout) -> 503. */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDownstreamUnreachable(ResourceAccessException ex) {
        log.warn("Downstream unreachable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(false, null, new ApiResponse.ErrorPayload(
                        "UPSTREAM_UNAVAILABLE", "Servis vaqtincha mavjud emas, keyinroq urinib ko'ring")));
    }

    @ExceptionHandler(OrderProxyException.class)
    public ResponseEntity<ApiResponse<Object>> handleOrderProxy(OrderProxyException ex) {
        log.warn("Order proxy error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiResponse<>(false, null, new ApiResponse.ErrorPayload(
                        "ORDER_SERVICE_ERROR", ex.getMessage())));
    }

    @ExceptionHandler(UnauthorizedShopException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedShopException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, null, new ApiResponse.ErrorPayload(
                        "UNAUTHORIZED", ex.getMessage())));
    }
}
