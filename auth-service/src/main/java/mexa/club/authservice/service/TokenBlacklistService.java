package mexa.club.authservice.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token, long ttlSeconds) {
        if (token == null || token.isBlank() || ttlSeconds <= 0) {
            return;
        }
        long expiresAtEpochSecond = (System.currentTimeMillis() / 1000L) + ttlSeconds;
        blacklist.put(token, expiresAtEpochSecond);
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Long expiresAt = blacklist.get(token);
        if (expiresAt == null) {
            return false;
        }
        long nowEpochSecond = System.currentTimeMillis() / 1000L;
        if (expiresAt <= nowEpochSecond) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredTokens() {
        long nowEpochSecond = System.currentTimeMillis() / 1000L;
        for (Map.Entry<String, Long> entry : blacklist.entrySet()) {
            if (entry.getValue() <= nowEpochSecond) {
                blacklist.remove(entry.getKey(), entry.getValue());
            }
        }
    }
}
