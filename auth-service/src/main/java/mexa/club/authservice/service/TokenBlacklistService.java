package mexa.club.authservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * M-13: chiqarilgan (logout) tokenlar Redis'da saqlanadi — TTL token exp bo'yicha.
 * Klasterda barcha podlar uchun umumiy; pod qayta ishga tushsa ham saqlanadi.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    public TokenBlacklistService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void blacklist(String token, long ttlSeconds) {
        if (token == null || token.isBlank() || ttlSeconds <= 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue()
                    .set(KEY_PREFIX + token, "1", Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            // Fail-closed: logout xati yashirilmasin — lekin token Redis'siz qolmasin.
            log.error("Token blacklist yozilmadi (Redis): {}", ex.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(
                    stringRedisTemplate.hasKey(KEY_PREFIX + token));
        } catch (Exception ex) {
            // Fail-closed: Redis o'chganida logout tokenlar ishlashda qolmasin.
            log.error("Token blacklist o'qilmadi (Redis) — fail-closed: {}", ex.getMessage());
            return true;
        }
    }
}
