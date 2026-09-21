package mexa.club.authservice.service;

import mexa.club.authservice.config.JwtSessionProperties;
import mexa.club.authservice.entity.RefreshToken;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.repository.RefreshTokenRepository;
import mexa.club.authservice.repository.UserRepository;
import mexa.club.authservice.security.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class RefreshTokenService {

    public record TokenPair(String accessToken, String refreshToken, UUID sessionId, long expiresInSeconds, String tokenType) {}

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserSessionService userSessionService;
    private final JwtSessionProperties jwtSessionProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtUtil jwtUtil,
            UserSessionService userSessionService,
            JwtSessionProperties jwtSessionProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userSessionService = userSessionService;
        this.jwtSessionProperties = jwtSessionProperties;
    }

    @Transactional
    public TokenPair issueForUsername(String username, String deviceInfo, String ipAddress, String deviceId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        return issueForUser(user, deviceInfo, ipAddress, deviceId);
    }

    @Transactional
    public TokenPair rotate(String rawRefreshToken, String deviceInfo, String ipAddress, String deviceId) {
        String normalized = normalizeRawToken(rawRefreshToken);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String hash = hash(normalized);
        if (userSessionService.findActiveSessionIdByRefreshTokenHash(hash).isEmpty()) {
            throw new IllegalArgumentException("Session is inactive");
        }

        RefreshToken stored = refreshTokenRepository
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(hash, now)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is invalid or expired"));

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found for refresh token"));
        if (!user.isEnabled() || !user.isVerified()) {
            throw new IllegalArgumentException("User is disabled or not verified");
        }

        // Qurilmaga bog'lash: saqlangan deviceId bilan kelgan deviceId mos kelishi kerak.
        // Mos kelmasa — potensial o'g'irlangan token; sessiyani va tokenni bekor qilamiz.
        var session = userSessionService.findByRefreshTokenHashAndActiveTrue(hash).orElse(null);
        if (session != null && !userSessionService.isDeviceAllowed(session, deviceId)) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            userSessionService.deactivateByRefreshTokenHash(hash);
            throw new mexa.club.authservice.exception.RefreshTokenDeviceMismatchException();
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String nextRefreshToken = generateRawRefreshToken();
        String nextRefreshHash = hash(nextRefreshToken);
        LocalDateTime refreshExpiresAt = now.plusSeconds(jwtSessionProperties.getRefreshTokenTtl());
        var updatedSession = userSessionService.rotate(hash, nextRefreshHash, refreshExpiresAt, deviceInfo, ipAddress, deviceId);

        String accessToken = jwtUtil.generateToken(user);
        persistRefreshToken(user.getId(), nextRefreshHash, refreshExpiresAt, updatedSession.getId());
        return new TokenPair(
                accessToken,
                nextRefreshToken,
                updatedSession.getId(),
                jwtSessionProperties.getAccessTokenTtl(),
                "Bearer"
        );
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = hash(rawRefreshToken.trim());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash).orElse(null);
        if (stored != null && !stored.isRevoked()) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
        }
        // Sessiyani deaktiv qilish: avval joriy hash orqali. Agar topilmasa
        // (refresh aylantirilgan — eski token kelgan) sessiya ID si orqali o'chiramiz,
        // aks holda sessiya aylanib "faol" bo'lib qolaveradi va soni oshib ketadi.
        if (!userSessionService.deactivateByRefreshTokenHash(hash)
                && stored != null && stored.getSessionId() != null) {
            userSessionService.deactivateById(stored.getSessionId());
        }
    }

    @Transactional(readOnly = true)
    public UUID resolveSessionId(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return null;
        }
        return userSessionService.findActiveSessionIdByRefreshTokenHash(hash(rawRefreshToken.trim())).orElse(null);
    }

    @Transactional
    public long cleanupExpiredTokens() {
        return refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private TokenPair issueForUser(User user, String deviceInfo, String ipAddress, String deviceId) {
        if (!user.isEnabled() || !user.isVerified()) {
            throw new IllegalArgumentException("User is disabled or not verified");
        }
        String refreshToken = generateRawRefreshToken();
        String refreshTokenHash = hash(refreshToken);
        LocalDateTime refreshExpiresAt = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(jwtSessionProperties.getRefreshTokenTtl());
        var session = userSessionService.create(user.getId(), deviceInfo, ipAddress, refreshTokenHash, refreshExpiresAt, deviceId);
        String accessToken = jwtUtil.generateToken(user);
        persistRefreshToken(user.getId(), refreshTokenHash, refreshExpiresAt, session.getId());
        return new TokenPair(accessToken, refreshToken, session.getId(), jwtSessionProperties.getAccessTokenTtl(), "Bearer");
    }

    private void persistRefreshToken(UUID userId, String refreshTokenHash, LocalDateTime expiresAt, UUID sessionId) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(refreshTokenHash);
        token.setExpiresAt(expiresAt);
        token.setRevoked(false);
        token.setSessionId(sessionId);
        refreshTokenRepository.save(token);
    }

    private String generateRawRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Refresh token hash failed", ex);
        }
    }

    private static String normalizeRawToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken is required");
        }
        return rawRefreshToken.trim();
    }
}
