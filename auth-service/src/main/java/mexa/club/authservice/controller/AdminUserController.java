package mexa.club.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.authservice.api.ApiResponse;
import mexa.club.authservice.dto.AdminCreateUserRequest;
import mexa.club.authservice.dto.AdminUserRolesRequest;
import mexa.club.authservice.dto.AdminUserPasswordChangeRequest;
import mexa.club.authservice.dto.AdminUserUpdateRequest;
import mexa.club.authservice.dto.BlockUserRequest;
import mexa.club.authservice.dto.UpdateMeRequest;
import mexa.club.authservice.dto.UserResponse;
import mexa.club.authservice.service.AdminUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Admin — User Management", description = "Admin endpoints for managing users and their roles. Most endpoints require SUPER_ADMIN privileges (USER_CREATE, USER_VIEW, USER_EDIT, USER_DELETE, USER_ROLE_ASSIGN authorities). All endpoints require a valid JWT Bearer token.")
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(
            AdminUserService adminUserService
    ) {
        this.adminUserService = adminUserService;
    }

    @Operation(
            summary = "Create user (admin, no OTP required)",
            description = """
                    SUPER_ADMIN only (requires USER_CREATE authority). Creates a user directly in the database, bypassing the OTP email-verification flow.
                    The password is BCrypt-hashed; the account is created as verified=true with provider=LOCAL.
                    If roleIds is omitted or empty, ROLE_USER is assigned. Otherwise the listed role UUIDs are applied (obtain them from GET /api/admin/roles).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="201", description = "User created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="400", description = "Validation error — missing or invalid fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="403", description = "Caller does not have USER_CREATE authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="409", description = "Email or username is already taken")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        UserResponse created = adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @Operation(
            summary = "List all users (paginated)",
            description = """
                    Returns a paginated, filterable list of all users. Page numbering starts at 0 — page=0 is the first page.
                    Optional query params: keyword (search in username/email), role (exact role name like ROLE_ADMIN),
                    enabled (true/false — false matns blocklangan foydalanuvchilarni beradi). Requires USER_VIEW authority.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "Paginated user list returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="403", description = "Caller does not have USER_VIEW authority")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers(
            @Parameter(description = "Foydalanuvchi nomi yoki email bo'yicha qidiruv", required = false) @RequestParam(required = false) String keyword,
            @Parameter(description = "Aniq rol nomi, masalan ROLE_ADMIN (ROLE_SUPER_ADMIN o'zi ko'rinishi uchun)", required = false) @RequestParam(required = false) String role,
            @Parameter(description = "true = faol, false = bloklangan", required = false) @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(okBody(adminUserService.listUsers(keyword, role, enabled, pageable)));
    }

    @Operation(
            summary = "List users by role name",
            description = "Returns all users that have the specified role. Requires USER_VIEW authority."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User list returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have USER_VIEW authority")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @GetMapping("/users/by-role")
    public ResponseEntity<Map<String, Object>> getUsersByRole(
            @Parameter(description = "Role name to filter by, e.g. ROLE_ADMIN", required = true) @RequestParam String role
    ) {
        return ResponseEntity.ok(okBody(adminUserService.listUsersByRole(role)));
    }

    @Operation(
            summary = "Get current authenticated user profile",
            description = "Returns the profile of the user identified by the provided JWT token. This is a convenience endpoint — it is distinguished from /users/{id} by the literal path segment 'me'."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "Current user profile returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        return ResponseEntity.ok(okBody(adminUserService.getCurrentUserProfile()));
    }

    @Operation(
            summary = "Update own profile (fullName)",
            description = "Allows any authenticated user to update their own fullName (ism-familiya). Used after Google login when fullName is missing."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "Profile updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/users/me")
    public ResponseEntity<Map<String, Object>> updateCurrentUser(@Valid @RequestBody UpdateMeRequest request) {
        return ResponseEntity.ok(okBody(adminUserService.updateCurrentUserFullName(request.fullName())));
    }

    @Operation(
            summary = "Find user by email",
            description = "Looks up a user by their email address. The email is trimmed and matched case-insensitively. Returns 404 if no user is found. Requires USER_VIEW authority."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "User found and returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="403", description = "Caller does not have USER_VIEW authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="404", description = "No user found with the given email")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @GetMapping(value = "/users/by-email", params = "email")
    public ResponseEntity<Map<String, Object>> getUserByEmail(
            @Parameter(description = "Qidiriladigan email", required = true, example = "superadmin@example.com")
            @RequestParam String email
    ) {
        return ResponseEntity.ok(okBody(adminUserService.getUserByEmail(email)));
    }

    @Operation(
            summary = "Get user by ID",
            description = "Returns the full profile of a single user by their UUID. Requires USER_VIEW authority."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "User found and returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="403", description = "Caller does not have USER_VIEW authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUser(
            @Parameter(description = "UUID of the user to retrieve", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(okBody(adminUserService.getUserById(id)));
    }

    @Operation(
            summary = "Update user profile",
            description = "Updates a user’s profile fields (username, email, enabled). Only the user themselves (token owner) or a caller with USER_EDIT authority can perform this action. Password changes are handled separately via /users/{id}/password."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "User updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="400", description = "Validation error — missing or invalid fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="403", description = "Caller is not the user themselves and does not have USER_EDIT authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="409", description = "The new username or email is already taken")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT') or @adminUserAccessService.canUpdateSelf(#id)")
    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @Parameter(description = "UUID of the user to update", required = true) @PathVariable UUID id,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return ResponseEntity.ok(okBody(adminUserService.updateUser(id, request)));
    }

    @Operation(
            summary = "Change own password",
            description = "Allows a user to change their own password by providing the current (old) password and the desired new password. Only the token owner can call this — not even a SUPER_ADMIN can change another user’s password via this endpoint."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "Password changed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="400", description = "Validation error — missing fields or new password does not meet requirements"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid, or old password is incorrect"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="403", description = "Caller is not the owner of this user account")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@adminUserAccessService.canUpdateSelf(#id)")
    @PutMapping("/users/{id}/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Parameter(description = "UUID of the user changing their password", required = true) @PathVariable UUID id,
            @Valid @RequestBody AdminUserPasswordChangeRequest request
    ) {
        return ResponseEntity.ok(okBody(adminUserService.changePassword(id, request)));
    }

    @Operation(
            summary = "Delete user",
            description = "Permanently deletes a user by their UUID. Requires USER_DELETE authority (SUPER_ADMIN)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "User deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="403", description = "Caller does not have USER_DELETE authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @Parameter(description = "UUID of the user to delete", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(okBody(adminUserService.deleteUser(id)));
    }

    @Operation(
            summary = "Block a user",
            description = "Disables a user account (enabled=false) and records the block reason and timestamp. A blocked user cannot log in. Requires USER_EDIT authority."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "User blocked successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="403", description = "Caller does not have USER_EDIT authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    @PostMapping("/users/{id}/block")
    public ResponseEntity<Map<String, Object>> blockUser(
            @Parameter(description = "UUID of the user to block", required = true) @PathVariable UUID id,
            @RequestBody(required = false) BlockUserRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(okBody(adminUserService.blockUser(id, reason)));
    }

    @Operation(
            summary = "Unblock a user",
            description = "Re-enables a blocked user account (enabled=true) and clears the block reason and timestamp. Requires USER_EDIT authority."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "User unblocked successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="403", description = "Caller does not have USER_EDIT authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    @PostMapping("/users/{id}/unblock")
    public ResponseEntity<Map<String, Object>> unblockUser(
            @Parameter(description = "UUID of the user to unblock", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(okBody(adminUserService.unblockUser(id)));
    }

    @Operation(
            summary = "Replace user roles",
            description = """
            Replaces the current roles of a user with the provided list of role UUIDs. Requires USER_ROLE_ASSIGN authority.

            Workflow:
            1. GET /api/admin/roles — retrieve the "id" (UUID) of each role you want to assign
            2. PUT /api/admin/users/{id}/roles — send those UUIDs in the roleIds array

            Correct request body:
            {
              "roleIds": ["f6169a9e-c3d1-476e-bbf1-87909ff81b50"]
            }

            If roleIds is omitted or empty, ROLE_USER is assigned.
            Note: send roleIds (UUIDs), not roleNames.
            For additive (merge) assignment, use POST /api/admin/roles/assign instead.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="200", description = "Roles assigned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="401", description = "JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="403", description = "Caller does not have USER_ROLE_ASSIGN authority"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode ="404", description = "User or one of the specified roles not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    @PutMapping("/users/{id}/roles")
    public ResponseEntity<Map<String, Object>> setRoles(
            @Parameter(description = "UUID of the user whose roles are being set", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Role UUIDs to assign. Get them from GET /api/admin/roles",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Assign ROLE_ADMIN",
                                    value = "{\"roleIds\": [\"22a1b467-f038-41cd-bd28-001d6812ec6c\"]}"
                            )
                    )
            )
            @Valid @RequestBody AdminUserRolesRequest request
    ) {
        return ResponseEntity.ok(okBody(adminUserService.setRoles(id, request)));
    }

    private static Map<String, Object> okBody(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        return body;
    }
}

