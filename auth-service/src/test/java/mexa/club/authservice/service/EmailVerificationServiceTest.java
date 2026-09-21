package mexa.club.authservice.service;

import mexa.club.authservice.config.EmailOtpProperties;
import mexa.club.authservice.entity.EmailOtp;
import mexa.club.authservice.entity.Role;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.exception.OtpExpiredException;
import mexa.club.authservice.exception.OtpInvalidException;
import mexa.club.authservice.exception.OtpTooManyAttemptsException;
import mexa.club.authservice.repository.EmailOtpRepository;
import mexa.club.authservice.repository.RoleRepository;
import mexa.club.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailOtpRepository otpRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmailService emailService;

    private EmailOtpProperties properties;

    @InjectMocks
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        properties = new EmailOtpProperties();
        properties.setTtlSeconds(300);
        properties.setResendMinIntervalSeconds(60);
        properties.setMaxAttempts(3);
        properties.setHashSecret("test-secret");
        service = new EmailVerificationService(otpRepository, userRepository, roleRepository, emailService, properties);
    }

    @Test
    void createAndSendRegistrationOtp_createsOtpAndSendsEmail() {
        service.createAndSendRegistrationOtp("user@example.com", "user1", "encoded-pass");

        ArgumentCaptor<EmailOtp> captor = ArgumentCaptor.forClass(EmailOtp.class);
        verify(otpRepository).save(captor.capture());
        EmailOtp saved = captor.getValue();

        assertEquals("user@example.com", saved.getEmail());
        assertEquals("user1", saved.getUsername());
        assertEquals("encoded-pass", saved.getPasswordHash());
        assertNotNull(saved.getCodeHash());
        assertEquals(64, saved.getCodeHash().length());
        assertNotNull(saved.getExpiresAt());
        verify(emailService).sendEmailVerificationOtp(eq("user@example.com"), any(String.class));
    }

    @Test
    void verifyEmail_whenExpired_throwsOtpExpiredExceptionAndDeletesOtp() {
        EmailOtp otp = new EmailOtp();
        otp.setEmail("user@example.com");
        otp.setUsername("user1");
        otp.setPasswordHash("encoded-pass");
        otp.setCodeHash("hash");
        otp.setAttemptsUsed(0);
        otp.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10));
        otp.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("user@example.com")).thenReturn(Optional.of(otp));

        assertThrows(OtpExpiredException.class, () -> service.verifyEmail("user@example.com", "123456"));
        verify(otpRepository).deleteByEmail("user@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyEmail_withInvalidCode_incrementsAttemptAndThrowsOtpInvalid() {
        EmailOtp otp = new EmailOtp();
        otp.setId(UUID.randomUUID());
        otp.setEmail("user@example.com");
        otp.setUsername("user1");
        otp.setPasswordHash("encoded-pass");
        otp.setCodeHash("expectedhash");
        otp.setAttemptsUsed(0);
        otp.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        otp.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("user@example.com")).thenReturn(Optional.of(otp));
        when(otpRepository.incrementAttemptsIfBelowMax(otp.getId(), properties.getMaxAttempts())).thenReturn(1);

        assertThrows(OtpInvalidException.class, () -> service.verifyEmail("user@example.com", "111111"));
        verify(otpRepository).incrementAttemptsIfBelowMax(otp.getId(), properties.getMaxAttempts());
    }

    @Test
    void verifyEmail_whenMaxAttemptsReached_throwsOtpTooManyAttempts() {
        EmailOtp otp = new EmailOtp();
        otp.setEmail("user@example.com");
        otp.setUsername("user1");
        otp.setPasswordHash("encoded-pass");
        otp.setCodeHash("expectedhash");
        otp.setAttemptsUsed(properties.getMaxAttempts());
        otp.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        otp.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("user@example.com")).thenReturn(Optional.of(otp));

        assertThrows(OtpTooManyAttemptsException.class, () -> service.verifyEmail("user@example.com", "111111"));
        verify(otpRepository, never()).save(any(EmailOtp.class));
    }

    @Test
    void verifyEmail_whenAtomicIncrementBlocked_throwsOtpTooManyAttempts() {
        EmailOtp otp = new EmailOtp();
        otp.setId(UUID.randomUUID());
        otp.setEmail("user@example.com");
        otp.setUsername("user1");
        otp.setPasswordHash("encoded-pass");
        otp.setCodeHash("expectedhash");
        otp.setAttemptsUsed(2);
        otp.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        otp.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("user@example.com")).thenReturn(Optional.of(otp));
        when(otpRepository.incrementAttemptsIfBelowMax(otp.getId(), properties.getMaxAttempts())).thenReturn(0);

        assertThrows(OtpTooManyAttemptsException.class, () -> service.verifyEmail("user@example.com", "111111"));
        verify(otpRepository).incrementAttemptsIfBelowMax(otp.getId(), properties.getMaxAttempts());
    }

    @Test
    void verifyEmail_withValidCode_createsVerifiedUser() {
        String email = "user@example.com";
        String otpCode = "123456";
        String expectedHash = sha256(properties.getHashSecret() + ":" + email + ":" + otpCode);
        EmailOtp otp = new EmailOtp(
                UUID.randomUUID(),
                email,
                "user1",
                "encoded-pass",
                expectedHash,
                0,
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(2)
        );
        Role roleUser = new Role(UUID.randomUUID(), "ROLE_USER", Set.of());

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(Optional.of(otp));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));

        assertDoesNotThrow(() -> service.verifyEmail(email, otpCode));
        verify(otpRepository).deleteByEmail(email);
        verify(userRepository).save(any(User.class));
    }

    private static String sha256(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
