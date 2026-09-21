package mexa.club.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mexa.club.authservice.dto.AssignRoleRequest;
import mexa.club.authservice.dto.PermissionCategoryResponse;
import mexa.club.authservice.dto.RoleCreateRequest;
import mexa.club.authservice.dto.RoleResponse;
import mexa.club.authservice.repository.UserRepository;
import mexa.club.authservice.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Role Management", description = "Endpoints for managing roles and permissions. All operations require ROLE_MANAGE or SYSTEM_CONFIG authority and a valid JWT Bearer token.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasAuthority('ROLE_MANAGE') or hasAuthority('SYSTEM_CONFIG')")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final UserRepository userRepository;

    @Operation(summary = "Create a new role", description = "Creates a new role with the specified name, description, and set of permissions. Requires ROLE_MANAGE or SYSTEM_CONFIG authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_MANAGE or SYSTEM_CONFIG authority"),
            @ApiResponse(responseCode = "409", description = "A role with that name already exists")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRole(@Valid @RequestBody RoleCreateRequest request) {
        RoleResponse created = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(created));
    }

    @Operation(summary = "List all roles", description = "Returns all roles defined in the system, including their assigned permissions. Requires ROLE_MANAGE or SYSTEM_CONFIG authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of roles returned successfully"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_MANAGE or SYSTEM_CONFIG authority")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRoles() {
        List<RoleResponse> roles = roleService.getAllRoles();
        return ResponseEntity.ok(okBody(roles));
    }

    @Operation(summary = "Get role by ID", description = "Returns a single role by its UUID, including its assigned permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role found and returned"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_MANAGE or SYSTEM_CONFIG authority"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRole(
            @Parameter(description = "UUID of the role to retrieve", required = true) @PathVariable UUID id
    ) {
        RoleResponse role = roleService.getRole(id);
        return ResponseEntity.ok(okBody(role));
    }

    @Operation(summary = "Update a role", description = "Updates the name, description, or permission set of an existing role. System-managed roles cannot be modified.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller lacks authority, or the target role is a system role that cannot be changed"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRole(
            @Parameter(description = "UUID of the role to update", required = true) @PathVariable UUID id,
            @Valid @RequestBody RoleCreateRequest request
    ) {
        RoleResponse updated = roleService.updateRole(id, request);
        return ResponseEntity.ok(okBody(updated));
    }

    @Operation(summary = "Delete a role", description = "Permanently deletes a role by its UUID. System-managed roles cannot be deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Role deleted successfully — no content returned"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller lacks authority, or the target role is a system role that cannot be deleted"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "UUID of the role to delete", required = true) @PathVariable UUID id
    ) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List all permissions grouped by category", description = "Returns all available permissions organised into named categories. Use this to discover permission names when constructing role creation or update requests.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions list returned, grouped by category"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_MANAGE or SYSTEM_CONFIG authority")
    })
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> getAllPermissionsGrouped() {
        List<PermissionCategoryResponse> categories = roleService.getAllPermissionsGrouped();
        return ResponseEntity.ok(okBody(categories));
    }

    @Operation(summary = "Assign roles to a user", description = "Assigns one or more roles to a user identified by their UUID. Existing roles are merged with the new ones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles assigned successfully"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_MANAGE or SYSTEM_CONFIG authority"),
            @ApiResponse(responseCode = "404", description = "User or one of the specified roles not found")
    })
    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignRolesToUser(@RequestBody AssignRoleRequest request) {
        roleService.assignRolesToUser(request.userId(), request.roleIds(), userRepository);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", Map.of("assigned", true, "userId", request.userId()));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Remove a role from a user", description = "Removes a single role from a user. Both the user UUID and the role UUID must be provided as path variables.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Role removed successfully — no content returned"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_MANAGE or SYSTEM_CONFIG authority"),
            @ApiResponse(responseCode = "404", description = "User or role not found")
    })
    @DeleteMapping("/assign/{userId}/{roleId}")
    public ResponseEntity<Void> removeRoleFromUser(
            @Parameter(description = "UUID of the user to remove the role from", required = true) @PathVariable UUID userId,
            @Parameter(description = "UUID of the role to remove", required = true) @PathVariable UUID roleId
    ) {
        roleService.removeRoleFromUser(userId, roleId, userRepository);
        return ResponseEntity.noContent().build();
    }

    private static Map<String, Object> okBody(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        return body;
    }
}
