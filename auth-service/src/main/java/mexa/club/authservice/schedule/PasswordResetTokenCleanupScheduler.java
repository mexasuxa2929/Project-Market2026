package mexa.club.authservice.schedule;

import lombok.extern.slf4j.Slf4j;
import mexa.club.authservice.service.PasswordResetService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.password-reset.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PasswordResetTokenCleanupScheduler {

    private final PasswordResetService passwordResetService;

    public PasswordResetTokenCleanupScheduler(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Scheduled(fixedRateString = "${app.password-reset.cleanup.scheduler-interval-ms:600000}")
    @Transactional
    public void cleanupExpiredPasswordResetTokens() {
        long removed = passwordResetService.cleanupExpiredTokens();
        if (removed > 0) {
            log.info("Removed {} expired password reset token(s)", removed);
        }
    }
}
