package mexa.club.shopservice.security;

import mexa.club.shopservice.exception.UnauthorizedShopException;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public final class ShopUsers {

    private ShopUsers() {
    }

    public static UUID requireUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedShopException("Authentication required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtUserPrincipal jwt && jwt.userId() != null) {
            return jwt.userId();
        }
        throw new UnauthorizedShopException("Invalid token (missing user id)");
    }
}
