package mexa.club.authservice.schedule;

import lombok.extern.slf4j.Slf4j;
import mexa.club.authservice.service.RefreshTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.refresh-token.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExpiredRefreshTokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    public ExpiredRefreshTokenCleanupScheduler(
            RefreshTokenService refreshTokenService
    ) {
        this.refreshTokenService = refreshTokenService;
    }

    @Scheduled(fixedRateString = "${app.refresh-token.cleanup.scheduler-interval-ms:600000}")
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        long removed = refreshTokenService.cleanupExpiredTokens();
        if (removed > 0) {
            log.info("Removed {} expired refresh token(s)", removed);
        }
    }
}
