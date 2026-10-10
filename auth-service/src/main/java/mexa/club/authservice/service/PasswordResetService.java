package mexa.club.authservice.service;

import mexa.club.authservice.config.PasswordResetProperties;
import mexa.club.authservice.entity.PasswordResetToken;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.repository.PasswordResetTokenRepository;
import mexa.club.authservice.repository.RefreshTokenRepository;
import mexa.club.authservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.NoSuchElementException;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetProperties properties;
    private final UserSessionService userSessionService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            PasswordResetProperties properties,
            UserSessionService userSessionService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.properties = properties;
        this.userSessionService = userSessionService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public boolean forgotPassword(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        return userRepository.findByEmailIgnoreCase(email).map(user -> {
            // M-7: resend interval — oxirgi kod yuborilganiga kamida
            // resendIntervalSeconds o'tmaguncha yangi kod yubormaymiz (bombardimondan himoya).
            // Javob foydalanuvchi uchun farq qilmaydi (aynan shu "yuborildi" ko'rinishi).
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            boolean resendSuppressed = passwordResetTokenRepository
                    .findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                    .filter(t -> t.getCreatedAt().plusSeconds(properties.getResendIntervalSeconds()).isAfter(now))
                    .isPresent();
            if (resendSuppressed) {
                log.debug("Password reset resend suppressed (interval) for user {}", user.getId());
                return true;
            }

            String rawCode = generateRawCode();
            passwordResetTokenRepository.deleteByUserIdAndUsedFalse(user.getId());

            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setTokenHash(hash(rawCode));
            token.setExpiresAt(now.plusMinutes(properties.getTtlMinutes()));
            token.setUsed(false);
            token.setAttemptsUsed(0);
            passwordResetTokenRepository.save(token);

            emailService.sendPasswordResetCode(user.getEmail(), rawCode);
            return true;
        }).orElse(false);
    }

    @Transactional
    public void verifyResetCode(String rawEmail, String rawCode) {
        resolveValidToken(rawEmail, rawCode);
    }

    @Transactional
    public void resetPassword(String rawEmail, String rawCode, String newPassword) {
        String normalizedPassword = normalizeRequired(newPassword, "newPassword");
        PasswordResetToken token = resolveValidToken(rawEmail, rawCode);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found for reset token"));

        user.setPassword(passwordEncoder.encode(normalizedPassword));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // M-8: parolni tiklagach eski sessiyalarni yopamiz — aks holda
        // hisob o'g'irlanganida hujumchi refresh tokeni bilan ishlayveradi.
        userSessionService.closeAll(user.getId());
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Transactional
    public long cleanupExpiredTokens() {
        return passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private PasswordResetToken resolveValidToken(String rawEmail, String rawCode) {
        String normalizedEmail = normalizeEmail(rawEmail);
        String normalizedCode = normalizeRequired(rawCode, "code");
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Reset code is invalid or expired"));

        PasswordResetToken token = passwordResetTokenRepository
                .findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .filter(t -> t.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new IllegalArgumentException("Reset code is invalid or expired"));

        // M-7: necha marta urinish mumkinligini cheklash — cheksiz tirnoqlashga qarshi himoya.
        if (token.getAttemptsUsed() >= properties.getMaxAttempts()) {
            token.setUsed(true);
            passwordResetTokenRepository.save(token);
            throw new IllegalArgumentException("Reset code is invalid or expired");
        }
        token.setAttemptsUsed(token.getAttemptsUsed() + 1);
        passwordResetTokenRepository.save(token);

        if (!token.getTokenHash().equals(hash(normalizedCode))) {
            throw new IllegalArgumentException("Reset code is invalid or expired");
        }
        return token;
    }

    private String generateRawCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Password reset token hash failed", ex);
        }
    }

    private static String normalizeEmail(String rawEmail) {
        String email = normalizeRequired(rawEmail, "email");
        return email.toLowerCase();
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
