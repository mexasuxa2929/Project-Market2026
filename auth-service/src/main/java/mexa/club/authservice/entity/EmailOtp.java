package mexa.club.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "email_otp",
        indexes = {
                @Index(name = "idx_email_otp_email", columnList = "email"),
                @Index(name = "idx_email_otp_expires_at", columnList = "expires_at"),
                @Index(name = "idx_email_otp_username", columnList = "username")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    /**
     * Tasdiqlashgacha ro‘yxatdan o‘tish: username va parol shu yerda (usersda yo‘q).
     */
    @Column(length = 100)
    private String username;

    /**
     * BCrypt hash — {@code PasswordEncoder} bilan solishtiriladi login da.
     */
    @Column(length = 255)
    private String passwordHash;

    /**
     * OTP kod sha256 hash ko‘rinishida saqlanadi.
     * 6-digit kod — plaintext saqlamaslik uchun.
     */
    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private int attemptsUsed;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}

