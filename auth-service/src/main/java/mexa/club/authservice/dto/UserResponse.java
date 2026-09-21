package mexa.club.authservice.dto;

import java.util.List;
import java.util.UUID;

/**
 * Foydalanuvchi profili (admin yoki boshqa API javoblar uchun).
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean enabled,
        boolean verified,
        List<String> roles
) {
}
