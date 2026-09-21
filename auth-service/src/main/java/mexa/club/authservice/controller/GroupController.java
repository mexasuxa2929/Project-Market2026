package mexa.club.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mexa.club.authservice.dto.AddMemberRequest;
import mexa.club.authservice.dto.GroupDetailResponse;
import mexa.club.authservice.dto.GroupMemberResponse;
import mexa.club.authservice.dto.GroupRequest;
import mexa.club.authservice.dto.GroupResponse;
import mexa.club.authservice.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Group Management", description = "Endpoints for managing user groups. All operations require GROUP_MANAGE authority.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/groups")
@PreAuthorize("hasAuthority('GROUP_MANAGE')")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "Create a new group", description = "Creates a new user group with the specified name and optional description.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Group created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "A group with that name already exists")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createGroup(@Valid @RequestBody GroupRequest request) {
        GroupResponse created = groupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(created));
    }

    @Operation(summary = "List all groups", description = "Returns all user groups with their member counts.")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllGroups() {
        List<GroupResponse> groups = groupService.getAllGroups();
        return ResponseEntity.ok(okBody(groups));
    }

    @Operation(summary = "Get group by ID", description = "Returns a single group with its full member list.")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getGroup(
            @Parameter(description = "UUID of the group", required = true) @PathVariable UUID id
    ) {
        GroupDetailResponse group = groupService.getGroup(id);
        return ResponseEntity.ok(okBody(group));
    }

    @Operation(summary = "Update a group", description = "Updates the name and/or description of an existing group.")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateGroup(
            @Parameter(description = "UUID of the group to update", required = true) @PathVariable UUID id,
            @Valid @RequestBody GroupRequest request
    ) {
        GroupResponse updated = groupService.updateGroup(id, request);
        return ResponseEntity.ok(okBody(updated));
    }

    @Operation(summary = "Delete a group", description = "Permanently deletes a group and all its member associations.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(
            @Parameter(description = "UUID of the group to delete", required = true) @PathVariable UUID id
    ) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add a member to a group", description = "Adds a user as a member of the specified group.")
    @PostMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> addMember(
            @Parameter(description = "UUID of the group", required = true) @PathVariable("id") UUID groupId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        GroupMemberResponse member = groupService.addMember(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(okBody(member));
    }

    @Operation(summary = "Remove a member from a group", description = "Removes a member from the specified group.")
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "UUID of the group", required = true) @PathVariable("id") UUID groupId,
            @Parameter(description = "UUID of the member to remove", required = true) @PathVariable UUID memberId
    ) {
        groupService.removeMember(groupId, memberId);
        return ResponseEntity.noContent().build();
    }

    private static Map<String, Object> okBody(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        return body;
    }
}