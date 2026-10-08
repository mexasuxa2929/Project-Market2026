package mexa.club.authservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login urinishlarini kuzatuvchi xizmat (H13).
 * 5 ta noto'g'ri urinishdan keyin 15 daqiqaga bloklaydi.
 *
 * Asosiy ombor — Redis (bir nechta auth instansiyasi uchun umumiy):
 *   login_attempts:{username}  — urinishlar soni (TTL 15 min)
 *   login_locked:{username}    — blok flagi (TTL 15 min)
 * Redis ishlamasa (pastda/tarmoq yo'q) — in-memory fallback bilan ishlashda davom etadi.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;
    private static final String ATTEMPT_PREFIX = "login_attempts:";
    private static final String LOCK_PREFIX = "login_locked:";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, AttemptInfo> fallback = new ConcurrentHashMap<>();

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLocked(String username) {
        if (username == null || username.isBlank()) return false;
        String key = normalize(username);
        try {
            return redisTemplate.hasKey(LOCK_PREFIX + key);
        } catch (Exception ex) {
            return fallbackIsLocked(key);
        }
    }

    public long getRemainingLockoutSeconds(String username) {
        if (username == null || username.isBlank()) return 0;
        String key = normalize(username);
        try {
            Long ttl = redisTemplate.getExpire(LOCK_PREFIX + key);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception ex) {
            AttemptInfo info = fallback.get(key);
            if (info == null || info.lockedUntil == null) return 0;
            long remaining = Duration.between(LocalDateTime.now(), info.lockedUntil).getSeconds();
            return Math.max(0, remaining);
        }
    }

    public void recordFailedAttempt(String username) {
        if (username == null || username.isBlank()) return;
        String key = normalize(username);
        try {
            String attemptKey = ATTEMPT_PREFIX + key;
            Long count = redisTemplate.opsForValue().increment(attemptKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(attemptKey, Duration.ofMinutes(LOCKOUT_MINUTES));
            }
            if (count != null && count >= MAX_ATTEMPTS) {
                redisTemplate.opsForValue().set(LOCK_PREFIX + key, "1", Duration.ofMinutes(LOCKOUT_MINUTES));
            }
        } catch (Exception ex) {
            recordFallback(key);
        }
    }

    public void resetAttempts(String username) {
        if (username == null || username.isBlank()) return;
        String key = normalize(username);
        try {
            redisTemplate.delete(ATTEMPT_PREFIX + key);
            redisTemplate.delete(LOCK_PREFIX + key);
        } catch (Exception ex) {
            fallback.remove(key);
        }
    }

    // ── In-memory fallback (Redis mavjud bo'lmasa) ────────────────────────

    private boolean fallbackIsLocked(String key) {
        AttemptInfo info = fallback.get(key);
        if (info == null) return false;
        if (info.lockedUntil != null && info.lockedUntil.isAfter(LocalDateTime.now())) {
            return true;
        }
        if (info.lockedUntil != null) {
            fallback.remove(key);
        }
        return false;
    }

    private void recordFallback(String key) {
        fallback.compute(key, (k, existing) -> {
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

    private static String normalize(String username) {
        return username.toLowerCase().trim();
    }

    private static class AttemptInfo {
        int failedAttempts = 0;
        LocalDateTime lockedUntil = null;
    }
}
