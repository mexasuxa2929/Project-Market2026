package mexa.club.authservice.mapper;

import mexa.club.authservice.dto.UserResponse;
import mexa.club.authservice.entity.Role;
import mexa.club.authservice.entity.User;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdminUserMapper {

    /** Sessiyasi so'nggi shuncha daqiqada ishlatilgan bo'lsa, foydalanuvchi "onlayn" deb hisoblanadi. */
    public static final int ONLINE_WINDOW_MINUTES = 15;

    /**
     * @param lastActiveAt  Foydalanuvchining eng so'nggi faol sessiyasi vaqti (UTC). <code>null</code>
     *                      bo'lsa "faollik ma'lumoti yo'q" holati qaytadi.
     */
    public Map<String, Object> toUserDto(User user, LocalDateTime lastActiveAt) {
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", user.getId());
        dto.put("username", user.getUsername());
        dto.put("email", user.getEmail());
        dto.put("fullName", user.getFullName());
        dto.put("enabled", user.isEnabled());
        dto.put("verified", user.isVerified());
        dto.put("provider", user.getProvider() != null ? user.getProvider() : "LOCAL");
        dto.put("roles", roles);
        if (user.getCreatedAt() != null) {
            dto.put("createdAt", user.getCreatedAt().toString());
        }
        if (lastActiveAt != null) {
            dto.put("lastActiveAt", lastActiveAt.toString());
            boolean online = Duration.between(lastActiveAt, LocalDateTime.now(ZoneOffset.UTC)).toMinutes() < ONLINE_WINDOW_MINUTES;
            dto.put("online", online);
        } else {
            dto.put("lastActiveAt", null);
            dto.put("online", false);
        }
        if (user.getBlockReason() != null) {
            dto.put("blockReason", user.getBlockReason());
        }
        if (user.getBlockedAt() != null) {
            dto.put("blockedAt", user.getBlockedAt().toString());
        }
        return dto;
    }

    public Map<String, Object> toUserDto(User user) {
        return toUserDto(user, null);
    }

    public UserResponse toUserResponse(User user) {
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.isVerified(),
                roles
        );
    }

}
