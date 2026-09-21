package mexa.club.authservice.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login urinishlarini kuzatuvchi xizmat.
 * 5 ta noto'g'ri urinishdan keyin 15 daqiqaga bloklaydi.
 */
@Service
public class LoginAttemptService {

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    public boolean isLocked(String username) {
        if (username == null || username.isBlank()) return false;
        String key = username.toLowerCase().trim();
        AttemptInfo info = attempts.get(key);
        if (info == null) return false;

        if (info.lockedUntil != null && info.lockedUntil.isAfter(LocalDateTime.now())) {
            return true;
        }
        if (info.lockedUntil != null && info.lockedUntil.isBefore(LocalDateTime.now())) {
            attempts.remove(key);
            return false;
        }
        return false;
    }

    public long getRemainingLockoutSeconds(String username) {
        if (username == null || username.isBlank()) return 0;
        String key = username.toLowerCase().trim();
        AttemptInfo info = attempts.get(key);
        if (info == null || info.lockedUntil == null) return 0;
        long remaining = java.time.Duration.between(LocalDateTime.now(), info.lockedUntil).getSeconds();
        return Math.max(0, remaining);
    }

    public void recordFailedAttempt(String username) {
        if (username == null || username.isBlank()) return;
        String key = username.toLowerCase().trim();
        attempts.compute(key, (k, existing) -> {
            if (existing == null) {
                AttemptInfo info = new AttemptInfo();
                info.failedAttempts = 1;
                if (MAX_ATTEMPTS <= 1) {
                    info.lockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
                }
                return info;
            }
            existing.failedAttempts++;
            if (existing.failedAttempts >= MAX_ATTEMPTS) {
                existing.lockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
            }
            return existing;
        });
    }

    public void resetAttempts(String username) {
        if (username == null || username.isBlank()) return;
        attempts.remove(username.toLowerCase().trim());
    }

    private static class AttemptInfo {
        int failedAttempts = 0;
        LocalDateTime lockedUntil = null;
    }
}
