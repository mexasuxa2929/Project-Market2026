package mexa.club.authservice.service;

import mexa.club.authservice.dto.UserSessionResponse;
import mexa.club.authservice.entity.UserSession;
import mexa.club.authservice.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    public UserSessionService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    @Transactional
    public UserSession create(UUID userId, String deviceInfo, String ipAddress, String refreshTokenHash, LocalDateTime expiresAt, String deviceId) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setDeviceInfo(trim(deviceInfo, 512));
        session.setIpAddress(trim(ipAddress, 128));
        session.setDeviceId(trim(deviceId, 256));
        session.setRefreshTokenHash(refreshTokenHash);
        session.setExpiresAt(expiresAt);
        session.setActive(true);
        return userSessionRepository.save(session);
    }

    @Transactional
    public UserSession rotate(
            String oldRefreshHash,
            String newRefreshHash,
            LocalDateTime newExpiresAt,
            String deviceInfo,
            String ipAddress,
            String deviceId
    ) {
        UserSession session = userSessionRepository.findByRefreshTokenHashAndActiveTrue(oldRefreshHash)
                .orElseThrow(() -> new NoSuchElementException("Active session not found for refresh token"));
        session.setRefreshTokenHash(newRefreshHash);
        if (deviceInfo != null && !deviceInfo.isBlank()) {
            session.setDeviceInfo(trim(deviceInfo, 512));
        }
        if (ipAddress != null && !ipAddress.isBlank()) {
            session.setIpAddress(trim(ipAddress, 128));
        }
        // Legacy sessiyalar uchun birinchi refreshda deviceId ni bog'lab qo'yamiz.
        if (session.getDeviceId() == null && deviceId != null && !deviceId.isBlank()) {
            session.setDeviceId(trim(deviceId, 256));
        }
        session.setLastUsedAt(LocalDateTime.now(ZoneOffset.UTC));
        session.setExpiresAt(newExpiresAt);
        return userSessionRepository.save(session);
    }

    /**
     * Refresh tokenni faqat ro'yxatdan o'tgan qurilmada ishlatishga ruxsat berish.
     * - session.deviceId == null  -> legacy/desktop; agar so'rovda deviceId kelsa bog'lanadi (ruxat).
     * - session.deviceId != null va so'rovda deviceId yo'q -> rad (qurilmaga bog'langan token
     *   boshqa mijoz tomonidan ishlatilayotgani shubhali).
     * - session.deviceId != null va mos kelmasa -> rad.
     */
    public boolean isDeviceAllowed(UserSession session, String deviceId) {
        String stored = session.getDeviceId();
        if (stored == null) {
            return true;
        }
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        return stored.equals(deviceId);
    }

    /** Sessiyani refresh-token hash orqali deaktiv qiladi. Sessiya topilsa true qaytaradi. */
    @Transactional
    public boolean deactivateByRefreshTokenHash(String refreshTokenHash) {
        if (refreshTokenHash == null || refreshTokenHash.isBlank()) {
            return false;
        }
        var opt = userSessionRepository.findByRefreshTokenHashAndActiveTrue(refreshTokenHash);
        if (opt.isEmpty()) {
            return false;
        }
        UserSession session = opt.get();
        session.setActive(false);
        session.setLastUsedAt(LocalDateTime.now(ZoneOffset.UTC));
        userSessionRepository.save(session);
        return true;
    }

    /** Sessiyani ID orqali deaktiv qiladi (refresh aylantirilgan bo'lsa ham). Idempotent. */
    @Transactional
    public boolean deactivateById(UUID sessionId) {
        if (sessionId == null) {
            return false;
        }
        var opt = userSessionRepository.findById(sessionId);
        if (opt.isEmpty() || !opt.get().isActive()) {
            return false;
        }
        UserSession session = opt.get();
        session.setActive(false);
        session.setLastUsedAt(LocalDateTime.now(ZoneOffset.UTC));
        userSessionRepository.save(session);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findActiveSessionIdByRefreshTokenHash(String refreshTokenHash) {
        return userSessionRepository.findByRefreshTokenHashAndActiveTrue(refreshTokenHash)
                .map(UserSession::getId);
    }

    @Transactional(readOnly = true)
    public Optional<UserSession> findByRefreshTokenHashAndActiveTrue(String refreshTokenHash) {
        return userSessionRepository.findByRefreshTokenHashAndActiveTrue(refreshTokenHash);
    }

    @Transactional(readOnly = true)
    public List<UserSessionResponse> listActiveByUser(UUID userId, UUID currentSessionId) {
        return userSessionRepository.findByUserIdAndActiveTrueOrderByLastUsedAtDesc(userId).stream()
                .map(s -> UserSessionResponse.fromEntity(s, currentSessionId))
                .toList();
    }

    @Transactional
    public void closeOne(UUID userId, UUID sessionId) {
        UserSession session = userSessionRepository.findByIdAndUserIdAndActiveTrue(sessionId, userId)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));
        session.setActive(false);
        session.setLastUsedAt(LocalDateTime.now(ZoneOffset.UTC));
        userSessionRepository.save(session);
    }

    @Transactional
    public void closeAll(UUID userId) {
        List<UserSession> sessions = userSessionRepository.findByUserIdAndActiveTrueOrderByLastUsedAtDesc(userId);
        if (sessions.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (UserSession session : sessions) {
            session.setActive(false);
            session.setLastUsedAt(now);
        }
        userSessionRepository.saveAll(sessions);
    }

    private static String trim(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.length() <= maxLen) {
            return v;
        }
        return v.substring(0, maxLen);
    }
}
