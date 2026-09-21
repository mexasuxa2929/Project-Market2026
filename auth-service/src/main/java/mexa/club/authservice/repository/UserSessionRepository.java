package mexa.club.authservice.repository;

import mexa.club.authservice.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    List<UserSession> findByUserIdAndActiveTrueOrderByLastUsedAtDesc(UUID userId);

    Optional<UserSession> findByIdAndUserIdAndActiveTrue(UUID id, UUID userId);

    Optional<UserSession> findByRefreshTokenHashAndActiveTrue(String refreshTokenHash);

    long deleteByExpiresAtBefore(LocalDateTime threshold);

    /** Barcha (tizim bo'yicha) muddati o'tmagan faol sessiyalar soni. */
    @Query("""
            select count(s)
            from UserSession s
            where s.active = true and (s.expiresAt is null or s.expiresAt > :now)
            """)
    long countActiveNotExpired(@Param("now") LocalDateTime now);

    /** Berilgan foydalanuvchilar uchun eng so'nggi faollik vaqtini qaytaradi. */
    @Query("""
            select s.userId as userId, max(coalesce(s.lastUsedAt, s.createdAt)) as lastActiveAt
            from UserSession s
            where s.active = true and s.userId in :userIds
            group by s.userId
            """)
    List<UserIdLastActive> findLastActiveByUserIds(@Param("userIds") Collection<UUID> userIds);

    interface UserIdLastActive {
        UUID getUserId();

        LocalDateTime getLastActiveAt();
    }
}
