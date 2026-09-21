package mexa.club.authservice.service;

import mexa.club.authservice.dto.AssignRoleRequest;
import mexa.club.authservice.dto.PermissionCategoryResponse;
import mexa.club.authservice.dto.RoleCreateRequest;
import mexa.club.authservice.dto.RoleResponse;
import mexa.club.authservice.repository.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RoleService {

    RoleResponse createRole(RoleCreateRequest request);

    RoleResponse updateRole(UUID id, RoleCreateRequest request);

    void deleteRole(UUID id);

    RoleResponse getRole(UUID id);

    List<RoleResponse> getAllRoles();

    List<PermissionCategoryResponse> getAllPermissionsGrouped();

    void assignRolesToUser(UUID userId, Set<UUID> roleIds, UserRepository userRepository);

    void removeRoleFromUser(UUID userId, UUID roleId, UserRepository userRepository);

    Set<String> getUserPermissions(UUID userId, UserRepository userRepository);
}
