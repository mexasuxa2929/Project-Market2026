package mexa.club.authservice.service;

import mexa.club.authservice.dto.AdminCreateUserRequest;
import mexa.club.authservice.dto.AdminUserPasswordChangeRequest;
import mexa.club.authservice.dto.AdminUserRolesRequest;
import mexa.club.authservice.dto.AdminUserUpdateRequest;
import mexa.club.authservice.dto.UserResponse;
import mexa.club.authservice.entity.Role;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.mapper.AdminUserMapper;
import mexa.club.authservice.realtime.RealtimeEventPublisher;
import mexa.club.authservice.repository.RoleRepository;
import mexa.club.authservice.repository.UserRepository;
import mexa.club.authservice.repository.UserSessionRepository;
import mexa.club.authservice.repository.RefreshTokenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminUserService {
    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_WAREHOUSE = "ROLE_WAREHOUSE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminUserMapper adminUserMapper;
    private final UserSessionRepository userSessionRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final UserSessionService userSessionService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AdminUserMapper adminUserMapper,
            UserSessionRepository userSessionRepository,
            RealtimeEventPublisher realtimeEventPublisher,
            UserSessionService userSessionService,
            RefreshTokenRepository refreshTokenRepository,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUserMapper = adminUserMapper;
        this.userSessionRepository = userSessionRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.userSessionService = userSessionService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        if (username.isEmpty()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (email.isEmpty()) {
            throw new IllegalArgumentException("email must not be blank");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(request.isEnabled());
        user.setVerified(true);
        user.setProvider("LOCAL");

        Set<UUID> uniqueRoleIds = request.getRoleIds() == null
                ? Set.of()
                : request.getRoleIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniqueRoleIds.isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER not configured"));
            user.setRoles(new LinkedHashSet<>(List.of(userRole)));
        } else {
            List<Role> rolesFound = roleRepository.findAllById(uniqueRoleIds).stream().toList();
            if (rolesFound.size() != uniqueRoleIds.size()) {
                throw new NoSuchElementException("One or more roles were not found");
            }
            user.setRoles(new LinkedHashSet<>(rolesFound));
        }

        User saved = userRepository.save(user);
        UserResponse response = adminUserMapper.toUserResponse(saved);
        realtimeEventPublisher.publishMutation("POST", "/api/admin/users");
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listUsers(String keyword, String role, Boolean enabled, Pageable pageable) {
        boolean excludeSuperAdmin = !ROLE_SUPER_ADMIN.equals(role);
        Specification<User> spec = Specification.where(searchSpec(keyword))
                .and(enabledSpec(enabled))
                .and(role != null && !role.isBlank() ? roleInNames(List.of(role)) : null);
        if (excludeSuperAdmin) {
            spec = spec.and(notInRole(ROLE_SUPER_ADMIN));
        }

        Page<User> page = userRepository.findAll(spec, pageable);
        Map<UUID, LocalDateTime> lastActiveByUser = lastActiveByUsers(
                page.getContent().stream().map(User::getId).toList());

        List<Map<String, Object>> items = page.getContent().stream()
                .map(u -> adminUserMapper.toUserDto(u, lastActiveByUser.get(u.getId())))
                .toList();

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("page", page.getNumber());
        data.put("size", page.getSize());
        data.put("totalElements", page.getTotalElements());
        data.put("totalPages", page.getTotalPages());
        data.put("first", page.isFirst());
        data.put("last", page.isLast());
        // Haqiqiy statistikalar — dashboard KPI'lari sahifa/tizim bo'yicha GLOBAL,
        // ro'yxat filterlari (qidiruv/holat/rol) ta'sir qilmaydi.
        data.put("activeSessions", userSessionRepository.countActiveNotExpired(now));
        data.put("adminCount", userRepository.count(roleInNames(List.of(ROLE_ADMIN, ROLE_SUPER_ADMIN))));
        data.put("warehouseCount", userRepository.count(roleInNames(List.of(ROLE_WAREHOUSE))));
        return data;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUsersByRole(String roleName) {
        List<User> users = userRepository.findByRoles_Name(roleName);
        Map<UUID, LocalDateTime> lastActiveByUser = lastActiveByUsers(
                users.stream().map(User::getId).toList());
        return users.stream()
                .map(u -> adminUserMapper.toUserDto(u, lastActiveByUser.get(u.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }
        String username = auth.getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw new IllegalStateException("Not authenticated");
        }
        User user = userRepository.findByUsernameFetchingRoles(username)
                .orElseThrow(() -> new NoSuchElementException("User not found for current session: " + username));
        return adminUserMapper.toUserDto(user);
    }

    @Transactional
    public Map<String, Object> updateCurrentUserFullName(String fullName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsernameFetchingRoles(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        user.setFullName(fullName.trim());
        userRepository.save(user);
        return adminUserMapper.toUserDto(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserByEmail(String email) {
        String normalized = email != null ? email.trim() : "";
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        User user = (currentUserIsSuperAdmin()
                ? userRepository.findByEmailIgnoreCase(normalized)
                : userRepository.findByEmailIgnoreCaseExcludingRole(normalized, ROLE_SUPER_ADMIN))
                .orElseThrow(() -> new NoSuchElementException("User not found for email: " + normalized));
        return adminUserMapper.toUserDto(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserById(UUID id) {
        User user = (currentUserIsSuperAdmin()
                ? userRepository.findById(id)
                : userRepository.findByIdExcludingRole(id, ROLE_SUPER_ADMIN))
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
        return adminUserMapper.toUserDto(user, lastActiveByUsers(List.of(id)).get(id));
    }

    @Transactional
    public Map<String, Object> updateUser(UUID id, AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            userRepository.findByUsername(request.getUsername())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Username is already taken");
                    });
            user.setUsername(request.getUsername().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            userRepository.findByEmail(request.getEmail())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Email is already in use");
                    });
            user.setEmail(request.getEmail().trim());
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        userRepository.save(user);
        Map<String, Object> dto = adminUserMapper.toUserDto(user);
        realtimeEventPublisher.publishMutation("PUT", "/api/admin/users/" + id);
        return dto;
    }

    @Transactional
    public Map<String, Object> changePassword(UUID id, AdminUserPasswordChangeRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
            throw new IllegalArgumentException("oldPassword must not be blank");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("changed", true);
        payload.put("id", id);
        return payload;
    }

    @Transactional
    public Map<String, Object> deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
        userRepository.delete(user);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deleted", true);
        payload.put("id", id);
        realtimeEventPublisher.publishMutation("DELETE", "/api/admin/users/" + id);
        return payload;
    }

    @Transactional
    public Map<String, Object> blockUser(UUID id, String reason) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
        user.setEnabled(false);
        user.setBlockReason(reason != null ? reason.trim() : null);
        user.setBlockedAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);

        // Foydalanuvchining barcha faol sessiyalarini bekor qilish
        userSessionService.closeAll(id);
        // Foydalanuvchining barcha refresh tokenlarini bekor qilish
        refreshTokenRepository.revokeAllByUserId(id);

        // Redis'ga bloklangan user'ni yozish — gateway darhol bloklaydi
        try {
            stringRedisTemplate.opsForValue().set("blocked:" + id, "1");
        } catch (Exception ignored) {}

        realtimeEventPublisher.publishMutation("POST", "/api/admin/users/" + id + "/block");

        Map<String, Object> dto = adminUserMapper.toUserDto(user);
        dto.put("blocked", true);
        return dto;
    }

    @Transactional
    public Map<String, Object> unblockUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
        user.setEnabled(true);
        user.setBlockReason(null);
        user.setBlockedAt(null);
        userRepository.save(user);

        // Redis'dan blokni o'chirish
        try {
            stringRedisTemplate.delete("blocked:" + id);
        } catch (Exception ignored) {}

        realtimeEventPublisher.publishMutation("POST", "/api/admin/users/" + id + "/unblock");

        Map<String, Object> dto = adminUserMapper.toUserDto(user);
        dto.put("blocked", false);
        return dto;
    }

    @Transactional
    public Map<String, Object> setRoles(UUID id, AdminUserRolesRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        Set<UUID> uniqueRoleIds = request.getRoleIds() == null
                ? Set.of()
                : request.getRoleIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniqueRoleIds.isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER not configured"));
            user.setRoles(new LinkedHashSet<>(List.of(userRole)));
        } else {
            List<Role> rolesFound = roleRepository.findAllById(uniqueRoleIds).stream().toList();
            if (rolesFound.size() != uniqueRoleIds.size()) {
                throw new NoSuchElementException("One or more roles were not found");
            }

            user.setRoles(new LinkedHashSet<>(rolesFound));
        }
        userRepository.save(user);
        return adminUserMapper.toUserDto(user);
    }

    private static boolean currentUserIsSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    // ── Dynamic filter (Specification) yordamchilari ───────────────────────────

    private static Specification<User> searchSpec(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String like = "%" + keyword.trim().toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), like, '\\'),
                    cb.like(cb.lower(root.get("email")), like, '\\')
            );
        };
    }

    private static Specification<User> enabledSpec(Boolean enabled) {
        return (root, query, cb) -> enabled == null ? null : cb.equal(root.get("enabled"), enabled);
    }

    /** Berilgan rollardan kamida bittasiga ega foydalanuvchilar (korrelyatsiyalangan subquery orqali). */
    private static Specification<User> roleInNames(Collection<String> roleNames) {
        return (root, query, cb) -> {
            if (roleNames == null || roleNames.isEmpty()) return null;
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<User> subRoot = subquery.from(User.class);
            Join<Object, Object> subRoles = subRoot.join("roles");
            subquery.select(cb.literal(1L))
                    .where(cb.equal(subRoot.get("id"), root.get("id")), subRoles.get("name").in(roleNames));
            return cb.exists(subquery);
        };
    }

    /** Berilgan rolga ega bo'lmagan foydalanuvchilar. */
    private static Specification<User> notInRole(String roleName) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<User> subRoot = subquery.from(User.class);
            Join<Object, Object> subRoles = subRoot.join("roles");
            subquery.select(cb.literal(1L))
                    .where(cb.equal(subRoot.get("id"), root.get("id")), cb.equal(subRoles.get("name"), roleName));
            return cb.not(cb.exists(subquery));
        };
    }

    private Map<UUID, LocalDateTime> lastActiveByUsers(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return userSessionRepository.findLastActiveByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        UserSessionRepository.UserIdLastActive::getUserId,
                        UserSessionRepository.UserIdLastActive::getLastActiveAt));
    }
}
