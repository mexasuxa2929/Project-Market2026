package mexa.club.authservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_google_id", columnList = "googleId")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "roles")
public class User {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Foydalanuvchining to'liq ismi (ixtiyoriy). Agar bo'sh bo'lsa,
     * UI username dan fallback ishlatadi.
     */
    @Column(length = 255)
    private String fullName;

    @Column(unique = true, length = 128)
    private String googleId;

    @Column(nullable = false, length = 16)
    private String provider = "LOCAL";

    @Column(length = 512)
    private String avatarUrl;

    private boolean enabled = true;

    /**
     * Email OTP bilan tasdiqlanishi holati.
     * {@code enabled} login uchun ishlatiladi; verificationdan keyin enabled=true qilinadi.
     */
    private boolean verified = false;

    /**
     * Ro’yxatdan o’tish vaqti (rejalashtirilgan tozalash uchun).
     */
    @Column(nullable = true, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Bloklash sababi (SUPER_ADMIN tomonidan to’ldiriladi).
     * enabled=false bo’lganda bu maydon saqlanadi.
     */
    @Column(length = 512)
    private String blockReason;

    /**
     * Bloklangan vaqt.
     */
    @Column(nullable = true)
    private LocalDateTime blockedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @PrePersist
    void prePersistCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
        if (provider == null || provider.isBlank()) {
            provider = "LOCAL";
        }
    }
}
