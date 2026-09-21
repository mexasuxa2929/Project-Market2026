package mexa.club.analyticsservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AnalyticsException.class)
    public ResponseEntity<Map<String, Object>> handleAnalytics(AnalyticsException ex) {
        log.warn("Analytics error [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of(
                        "success", false,
                        "code", ex.getCode(),
                        "message", ex.getMessage()
                ));
    }
}
