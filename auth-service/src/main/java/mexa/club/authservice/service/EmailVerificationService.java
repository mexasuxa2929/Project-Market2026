package mexa.club.authservice.service;

import mexa.club.authservice.config.EmailOtpProperties;
import mexa.club.authservice.entity.EmailOtp;
import mexa.club.authservice.entity.Role;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.exception.OtpExpiredException;
import mexa.club.authservice.exception.OtpInvalidException;
import mexa.club.authservice.exception.OtpNotFoundException;
import mexa.club.authservice.exception.OtpTooManyAttemptsException;
import mexa.club.authservice.exception.ResendRateLimitedException;
import mexa.club.authservice.repository.EmailOtpRepository;
import mexa.club.authservice.repository.RoleRepository;
import mexa.club.authservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class EmailVerificationService {

    private final EmailOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final EmailOtpProperties properties;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(
            EmailOtpRepository otpRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            EmailService emailService,
            EmailOtpProperties properties
    ) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.properties = properties;
    }

    /**
     * Ro‘yxatdan o‘tish: barcha ma’lumot faqat {@code email_otp}da; {@code users}ga faqat verify dan keyin.
     */
    @Transactional
    public void createAndSendRegistrationOtp(String email, String username, String encodedPassword) {
        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // Qayta ro‘yxatdan o‘tish (bir xil email) darhol yangi OTP — interval faqat resend-code da.
        otpRepository.deleteByEmail(normalizedEmail);

        String otpCode = generateOtpCode();
        String codeHash = hash(normalizedEmail, otpCode);

        EmailOtp entity = new EmailOtp();
        entity.setEmail(normalizedEmail);
        entity.setUsername(username);
        entity.setPasswordHash(encodedPassword);
        entity.setCodeHash(codeHash);
        entity.setAttemptsUsed(0);
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plusSeconds(properties.getTtlSeconds()));

        otpRepository.save(entity);

        emailService.sendEmailVerificationOtp(normalizedEmail, otpCode);
    }

    /**
     * Mavjud kutilayotgan ro‘yxatdan o‘tish uchun OTP ni qayta yuboradi (username/parol o‘zgarmaydi).
     */
    @Transactional
    public void resendVerificationOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            if (user.isEnabled()) {
                throw new OtpNotFoundException("Email already registered: " + normalizedEmail);
            }
        });

        EmailOtp current = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new OtpNotFoundException("No pending registration for email: " + normalizedEmail));
        if (current.getUsername() == null || current.getPasswordHash() == null) {
            throw new OtpNotFoundException("Incomplete pending registration for email: " + normalizedEmail);
        }

        applyResendRateLimit(normalizedEmail, now);

        String username = current.getUsername();
        String passwordHash = current.getPasswordHash();

        otpRepository.deleteByEmail(normalizedEmail);

        String otpCode = generateOtpCode();
        String codeHash = hash(normalizedEmail, otpCode);

        EmailOtp entity = new EmailOtp();
        entity.setEmail(normalizedEmail);
        entity.setUsername(username);
        entity.setPasswordHash(passwordHash);
        entity.setCodeHash(codeHash);
        entity.setAttemptsUsed(0);
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plusSeconds(properties.getTtlSeconds()));

        otpRepository.save(entity);

        emailService.sendEmailVerificationOtp(normalizedEmail, otpCode);
    }

    @Transactional
    public String verifyEmail(String email, String otpCode) {
        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new OtpNotFoundException("Email already registered: " + normalizedEmail);
        }

        EmailOtp latest = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new OtpNotFoundException("OTP not found for email: " + normalizedEmail));

        if (latest.getUsername() == null || latest.getPasswordHash() == null) {
            throw new OtpNotFoundException("Incomplete pending registration for email: " + normalizedEmail);
        }

        if (now.isAfter(latest.getExpiresAt())) {
            otpRepository.deleteByEmail(normalizedEmail);
            throw new OtpExpiredException("OTP expired");
        }

        int maxAttempts = properties.getMaxAttempts();
        if (latest.getAttemptsUsed() >= maxAttempts) {
            throw new OtpTooManyAttemptsException("Too many OTP attempts");
        }

        String providedHash = hash(normalizedEmail, otpCode);
        if (!providedHash.equals(latest.getCodeHash())) {
            int updatedRows = otpRepository.incrementAttemptsIfBelowMax(latest.getId(), maxAttempts);
            if (updatedRows == 0) {
                throw new OtpTooManyAttemptsException("Too many OTP attempts");
            }
            int attemptsAfterIncrement = latest.getAttemptsUsed() + 1;
            if (attemptsAfterIncrement >= maxAttempts) {
                throw new OtpTooManyAttemptsException("Too many OTP attempts");
            }
            throw new OtpInvalidException("OTP code is invalid");
        }

        otpRepository.deleteByEmail(normalizedEmail);

        User user = new User();
        user.setUsername(latest.getUsername());
        user.setEmail(normalizedEmail);
        user.setPassword(latest.getPasswordHash());
        user.setVerified(true);
        user.setEnabled(true);

        // Har bir yangi foydalanuvchi standart ROLE_USER rolini oladi.
        // Rol topilmasa — DataInitializer ishlamagan demak, exception tashlanadi.
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not configured in DataInitializer"));
        HashSet<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);
        return user.getUsername();
    }

    /**
     * Username bandmi: {@code users} yoki boshqa email uchun faol (muddati o‘tmagan) {@code email_otp}.
     */
    public boolean isUsernameBlockedForRegistration(String username, String normalizedEmail) {
        if (userRepository.findByUsername(username).isPresent()) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return otpRepository.existsActivePendingForOtherEmail(username, now, normalizedEmail);
    }

    private void applyResendRateLimit(String normalizedEmail, LocalDateTime now) {
        Optional<EmailOtp> latestOpt = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail);
        if (latestOpt.isEmpty()) {
            return;
        }
        EmailOtp latest = latestOpt.get();
        long secondsSinceCreated = now.toEpochSecond(ZoneOffset.UTC) - latest.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
        if (secondsSinceCreated < properties.getResendMinIntervalSeconds()) {
            long waitSeconds = properties.getResendMinIntervalSeconds() - secondsSinceCreated;
            throw new ResendRateLimitedException(
                    Math.max(0, waitSeconds),
                    "Too many resend requests. Try again in " + Math.max(0, waitSeconds) + " seconds."
            );
        }
    }

    private String generateOtpCode() {
        int code = 100000 + random.nextInt(900000);
        return String.format("%06d", code);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("email must not be null");
        }
        String t = email.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        return t.toLowerCase();
    }

    private String hash(String email, String otpCode) {
        String raw = properties.getHashSecret() + ":" + email + ":" + otpCode;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("OTP hash failed", ex);
        }
    }
}
