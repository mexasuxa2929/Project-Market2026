package mexa.club.authservice.service;

import lombok.RequiredArgsConstructor;
import mexa.club.authservice.dto.PermissionCategoryResponse;
import mexa.club.authservice.dto.PermissionResponse;
import mexa.club.authservice.dto.RoleCreateRequest;
import mexa.club.authservice.dto.RoleResponse;
import mexa.club.authservice.entity.Permission;
import mexa.club.authservice.entity.Role;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.exception.RoleAlreadyExistsException;
import mexa.club.authservice.exception.RoleNotFoundException;
import mexa.club.authservice.exception.SystemRoleException;
import mexa.club.authservice.repository.RoleRepository;
import mexa.club.authservice.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Write operations
    // ─────────────────────────────────────────────────────────────────────────

    private static final String SUPER_ADMIN_ROLE = "ROLE_SUPER_ADMIN";

    @Override
    public RoleResponse createRole(RoleCreateRequest request) {
        String normalizedName = normalizeRoleName(request.name());
        if (SUPER_ADMIN_ROLE.equals(normalizedName)) {
            throw new SystemRoleException(SUPER_ADMIN_ROLE + " faqat tizim migratsiyasi orqali yaratilishi mumkin");
        }
        if (roleRepository.existsByName(normalizedName)) {
            throw new RoleAlreadyExistsException("Role already exists: " + normalizedName);
        }

        Role role = new Role();
        role.setName(normalizedName);
        role.setDisplayName(request.displayName());
        role.setDescription(request.description());
        role.setSystem(false);
        role.setCreatedBy(currentUsername());
        role.setPermissions(parsePermissions(request.permissionNames()));

        return roleToResponse(roleRepository.save(role));
    }

    @Override
    public RoleResponse updateRole(UUID id, RoleCreateRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + id));

        if (SUPER_ADMIN_ROLE.equals(role.getName())) {
            throw new SystemRoleException(SUPER_ADMIN_ROLE + " rolini o'zgartirib bo'lmaydi");
        }
        if (role.isSystem()) {
            throw new SystemRoleException("Cannot modify system role: " + role.getName());
        }

        if (request.displayName() != null) role.setDisplayName(request.displayName());
        if (request.description() != null) role.setDescription(request.description());

        // Fully replace permission set
        role.setPermissions(parsePermissions(request.permissionNames()));

        return roleToResponse(roleRepository.save(role));
    }

    @Override
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + id));
        if (SUPER_ADMIN_ROLE.equals(role.getName())) {
            throw new SystemRoleException(SUPER_ADMIN_ROLE + " rolini o'chirib bo'lmaydi");
        }
        if (role.isSystem()) {
            throw new SystemRoleException("Cannot delete system role: " + role.getName());
        }
        roleRepository.delete(role);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read operations
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + id));
        if (SUPER_ADMIN_ROLE.equals(role.getName()) && !currentUserIsSuperAdmin()) {
            throw new SystemRoleException(SUPER_ADMIN_ROLE + " faqat SUPER_ADMIN o'zi ko'rishi mumkin");
        }
        return roleToResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        boolean isSuperAdmin = currentUserIsSuperAdmin();
        return roleRepository.findAll().stream()
                .filter(r -> !SUPER_ADMIN_ROLE.equals(r.getName()) || isSuperAdmin)
                .map(this::roleToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Groups all Permission enum values by their name prefix (first word before '_').
     * No DB call needed — the enum is the single source of truth.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PermissionCategoryResponse> getAllPermissionsGrouped() {
        Map<String, List<PermissionResponse>> grouped = Arrays.stream(Permission.values())
                .collect(Collectors.groupingBy(
                        p -> p.name().split("_")[0],   // e.g. PRODUCT_READ → "PRODUCT"
                        LinkedHashMap::new,
                        Collectors.mapping(
                                p -> new PermissionResponse(
                                        p.name(),
                                        toDisplayName(p.name()),
                                        p.name().split("_")[0]
                                ),
                                Collectors.toList()
                        )
                ));

        return grouped.entrySet().stream()
                .map(e -> new PermissionCategoryResponse(e.getKey(), e.getValue()))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User ↔ Role operations
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void assignRolesToUser(UUID userId, Set<UUID> roleIds, UserRepository userRepository) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        Set<Role> current = user.getRoles() != null
                ? new LinkedHashSet<>(user.getRoles())
                : new LinkedHashSet<>();

        if (roleIds == null || roleIds.isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER not configured"));
            current.add(userRole);
        } else {
            List<Role> roles = roleRepository.findAllById(roleIds);
            if (roles.size() != roleIds.size()) {
                throw new RoleNotFoundException("One or more roles not found");
            }
            current.addAll(roles);
        }

        user.setRoles(current);
        userRepository.save(user);
    }

    @Override
    public void removeRoleFromUser(UUID userId, UUID roleId, UserRepository userRepository) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));

        if (user.getRoles() != null) {
            user.getRoles().remove(role);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(UUID userId, UserRepository userRepository) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        if (user.getRoles() == null) {
            return Collections.emptySet();
        }

        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions() == null
                        ? java.util.stream.Stream.empty()
                        : role.getPermissions().stream())
                .map(Permission::name)
                .collect(Collectors.toSet());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static Set<Permission> parsePermissions(Set<String> names) {
        if (names == null || names.isEmpty()) return new HashSet<>();
        Set<Permission> result = new HashSet<>();
        for (String name : names) {
            try {
                result.add(Permission.valueOf(name));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown permission: " + name);
            }
        }
        return result;
    }

    private RoleResponse roleToResponse(Role role) {
        List<String> permissions = role.getPermissions() == null
                ? List.of()
                : role.getPermissions().stream()
                        .map(Permission::name)
                        .sorted()
                        .toList();

        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDisplayName(),
                role.getDescription(),
                role.isSystem(),
                permissions,
                role.getCreatedAt(),
                role.getCreatedBy()
        );
    }

    private static String toDisplayName(String name) {
        // "PRODUCT_READ" → "product read"
        return name.replace("_", " ").toLowerCase();
    }

    private static String normalizeRoleName(String raw) {
        String upper = (raw != null ? raw.trim() : "").toUpperCase(java.util.Locale.ROOT);
        if (upper.isEmpty()) return upper;
        return upper.startsWith("ROLE_") ? upper : "ROLE_" + upper;
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
    }

    private static boolean currentUserIsSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}
