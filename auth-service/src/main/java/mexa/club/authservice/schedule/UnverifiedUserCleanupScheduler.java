package mexa.club.authservice.schedule;

import lombok.extern.slf4j.Slf4j;
import mexa.club.authservice.config.UnverifiedUserCleanupProperties;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.repository.EmailOtpRepository;
import mexa.club.authservice.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Muddati o‘tgan kutilayotgan ro‘yxatdan o‘tishlar ({@code email_otp}) va (ixtiyoriy) eski modeldan qolgan
 * tasdiqlanmagan {@code users} yozuvlarini tozalaydi.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UnverifiedUserCleanupScheduler {

    private final UserRepository userRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final UnverifiedUserCleanupProperties properties;

    public UnverifiedUserCleanupScheduler(
            UserRepository userRepository,
            EmailOtpRepository emailOtpRepository,
            UnverifiedUserCleanupProperties properties
    ) {
        this.userRepository = userRepository;
        this.emailOtpRepository = emailOtpRepository;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${app.cleanup.scheduler-interval-ms:600000}")
    @Transactional
    public void removeStalePendingAndLegacyUsers() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        long expiredOtps = emailOtpRepository.deleteByExpiresAtBefore(now);
        if (expiredOtps > 0) {
            log.info("Removed {} expired email_otp row(s)", expiredOtps);
        }

        int maxAge = properties.getUnverifiedUserMaxAgeMinutes();
        if (maxAge <= 0) {
            return;
        }
        LocalDateTime cutoff = now.minusMinutes(maxAge);
        List<User> stale = userRepository.findByVerifiedFalseAndCreatedAtBefore(cutoff);
        if (stale.isEmpty()) {
            return;
        }
        List<String> emails = stale.stream().map(User::getEmail).distinct().collect(Collectors.toList());
        emailOtpRepository.deleteByEmailIn(emails);
        userRepository.deleteAll(stale);
        log.info("Removed {} legacy unverified user row(s) older than {} minutes", stale.size(), maxAge);
    }
}
