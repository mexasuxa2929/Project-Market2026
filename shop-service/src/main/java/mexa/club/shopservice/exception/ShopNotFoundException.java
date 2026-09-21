package mexa.club.shopservice.exception;

import java.util.UUID;

public class ShopNotFoundException extends RuntimeException {
    public ShopNotFoundException(String message) {
        super(message);
    }

    public ShopNotFoundException(UUID resourceId, String type) {
        super(type + " not found: " + resourceId);
    }
}
