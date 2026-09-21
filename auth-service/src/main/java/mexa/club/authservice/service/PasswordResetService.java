package mexa.club.authservice.service;

import mexa.club.authservice.config.PasswordResetProperties;
import mexa.club.authservice.entity.PasswordResetToken;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.repository.PasswordResetTokenRepository;
import mexa.club.authservice.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            PasswordResetProperties properties
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.properties = properties;
    }

    @Transactional
    public boolean forgotPassword(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        return userRepository.findByEmailIgnoreCase(email).map(user -> {
            String rawCode = generateRawCode();
            passwordResetTokenRepository.deleteByUserIdAndUsedFalse(user.getId());

            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setTokenHash(hash(rawCode));
            token.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(properties.getTtlMinutes()));
            token.setUsed(false);
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

        return passwordResetTokenRepository
                .findByUserIdAndTokenHashAndUsedFalseAndExpiresAtAfter(user.getId(), hash(normalizedCode), now)
                .orElseThrow(() -> new IllegalArgumentException("Reset code is invalid or expired"));
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
