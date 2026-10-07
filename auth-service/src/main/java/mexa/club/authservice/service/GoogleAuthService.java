package mexa.club.authservice.service;

import mexa.club.authservice.entity.Role;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.repository.RoleRepository;
import mexa.club.authservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final Random random = new Random();

    public GoogleAuthService(
            GoogleTokenVerifier googleTokenVerifier,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService
    ) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public RefreshTokenService.TokenPair authenticate(String idToken, String deviceInfo, String ipAddress, String deviceId) {
        GoogleTokenVerifier.GooglePrincipal principal = googleTokenVerifier.verify(idToken);
        User user;
        Optional<User> byGoogleId = userRepository.findByGoogleId(principal.googleId());
        if (byGoogleId.isPresent()) {
            // Google orqali yaratilgan (yoki avval Google'ga ulangan) hisob — desktop admin kirishi uchun admin rol kafolatlanadi.
            user = ensureAdminRole(byGoogleId.get());
        } else {
            user = userRepository.findByEmail(principal.email())
                    .map(existing -> linkExistingUser(existing, principal))
                    .orElseGet(() -> createGoogleUser(principal));
        }
        return refreshTokenService.issueForUsername(user.getUsername(), deviceInfo, ipAddress, deviceId);
    }

    /** Google hisobda admin rol yo'q bo'lsa qo'shadi (local hisob ulanishida kuchaytirilmaydi). */
    private User ensureAdminRole(User user) {
        boolean hasAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getName()) || "ROLE_SUPER_ADMIN".equals(r.getName()));
        if (hasAdmin) {
            return user;
        }
        Role admin = roleRepository.findByName("ROLE_ADMIN").orElse(null);
        if (admin == null) {
            return user;
        }
        Set<Role> roles = user.getRoles() != null ? new HashSet<>(user.getRoles()) : new HashSet<>();
        roles.add(admin);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    private User linkExistingUser(User user, GoogleTokenVerifier.GooglePrincipal principal) {
        user.setGoogleId(principal.googleId());
        user.setProvider("GOOGLE");
        if (principal.avatarUrl() != null && !principal.avatarUrl().isBlank()) {
            user.setAvatarUrl(principal.avatarUrl());
        }
        // Google dan kelgan ism bo'sh bo'lmasa va hozirgi fullName bo'sh/“null” bo'lsa to'ldirish
        if (principal.name() != null && !principal.name().isBlank()
                && (user.getFullName() == null || user.getFullName().isBlank()
                || user.getFullName().equalsIgnoreCase("null"))) {
            user.setFullName(principal.name().trim());
        }
        user.setEnabled(true);
        user.setVerified(true);
        return userRepository.save(user);
    }

    private User createGoogleUser(GoogleTokenVerifier.GooglePrincipal principal) {
        User user = new User();
        user.setEmail(principal.email());
        user.setGoogleId(principal.googleId());
        user.setProvider("GOOGLE");
        user.setAvatarUrl(principal.avatarUrl());
        if (principal.name() != null && !principal.name().isBlank()) {
            user.setFullName(principal.name().trim());
        }
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(true);
        user.setVerified(true);
        user.setUsername(generateUniqueUsername(principal.email()));
        user.setRoles(googleDefaultRoles());
        return userRepository.save(user);
    }

    /** Google orqali ro'yxatdan o'tgan yangi hisoblar admin panel uchun ROLE_ADMIN oladi. */
    private Set<Role> googleDefaultRoles() {
        Role roleAdmin = roleRepository.findByName("ROLE_ADMIN").orElse(null);
        if (roleAdmin != null) {
            return new HashSet<>(Set.of(roleAdmin));
        }
        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new NoSuchElementException("ROLE_USER not configured"));
        return new HashSet<>(Set.of(roleUser));
    }

    private String generateUniqueUsername(String email) {
        String local = email.split("@")[0];
        String normalized = local.replaceAll("[^A-Za-z0-9._-]", "");
        if (normalized.isBlank()) {
            normalized = "user";
        }
        String base = normalized.length() > 56 ? normalized.substring(0, 56) : normalized;
        String candidate;
        do {
            candidate = base + String.format("%04d", random.nextInt(10000));
        } while (userRepository.existsByUsername(candidate));
        return candidate;
    }
}
