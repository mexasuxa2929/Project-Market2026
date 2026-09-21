package mexa.club.authservice.repository;

import mexa.club.authservice.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalseAndExpiresAtAfter(String tokenHash, LocalDateTime now);

    Optional<PasswordResetToken> findByUserIdAndTokenHashAndUsedFalseAndExpiresAtAfter(
            UUID userId,
            String tokenHash,
            LocalDateTime now
    );

    long deleteByUserIdAndUsedFalse(UUID userId);

    long deleteByExpiresAtBefore(LocalDateTime now);
}
