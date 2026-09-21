package mexa.club.authservice.repository;

import mexa.club.authservice.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    Optional<EmailOtp> findTopByEmailOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);

    void deleteByEmailIn(Collection<String> emails);

    long deleteByExpiresAtBefore(LocalDateTime instant);

    @Modifying
    @Query("UPDATE EmailOtp o SET o.attemptsUsed = o.attemptsUsed + 1 "
            + "WHERE o.id = :otpId AND o.attemptsUsed < :maxAttempts")
    int incrementAttemptsIfBelowMax(
            @Param("otpId") UUID otpId,
            @Param("maxAttempts") int maxAttempts
    );

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM EmailOtp o WHERE o.username = :username "
            + "AND o.expiresAt > :now AND o.email <> :email")
    boolean existsActivePendingForOtherEmail(
            @Param("username") String username,
            @Param("now") LocalDateTime now,
            @Param("email") String email
    );
}

