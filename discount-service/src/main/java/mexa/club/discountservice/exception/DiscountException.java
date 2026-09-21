package mexa.club.discountservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DiscountException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public DiscountException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public static final String PROMOTION_NOT_FOUND = "PROMOTION_NOT_FOUND";
    public static final String PROMOTION_INACTIVE = "PROMOTION_INACTIVE";
    public static final String PROMOTION_EXPIRED = "PROMOTION_EXPIRED";
    public static final String PROMOTION_NOT_STARTED = "PROMOTION_NOT_STARTED";
    public static final String USAGE_LIMIT_REACHED = "USAGE_LIMIT_REACHED";
    public static final String USER_LIMIT_REACHED = "USER_LIMIT_REACHED";
    public static final String MIN_ORDER_AMOUNT_NOT_MET = "MIN_ORDER_AMOUNT_NOT_MET";
    public static final String PRODUCT_NOT_ELIGIBLE = "PRODUCT_NOT_ELIGIBLE";
    public static final String ORDER_ALREADY_DISCOUNTED = "ORDER_ALREADY_DISCOUNTED";
    public static final String INTERNAL_SECRET_INVALID = "INTERNAL_SECRET_INVALID";
    public static final String CONFIRM_MISMATCH = "CONFIRM_MISMATCH";
    public static final String USAGE_NOT_FOUND = "USAGE_NOT_FOUND";
}
