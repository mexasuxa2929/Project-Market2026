package mexa.club.authservice.security;

import mexa.club.authservice.entity.User;
import mexa.club.authservice.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service("adminUserAccessService")
public class AdminUserAccessService {

    private final UserRepository userRepository;

    public AdminUserAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Token egasi {@code targetUserId} bo‘yicha o‘zini yangilayotgan bo‘lsa true.
     */
    public boolean canUpdateSelf(UUID targetUserId) {
        if (targetUserId == null) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        String currentUsername = auth.getName();
        if (currentUsername == null || currentUsername.isBlank() || "anonymousUser".equals(currentUsername)) {
            return false;
        }

        Optional<User> current = userRepository.findByUsername(currentUsername);
        return current
                .map(User::getId)
                .filter(Objects::nonNull)
                .map(id -> id.equals(targetUserId))
                .orElse(false);
    }
}

