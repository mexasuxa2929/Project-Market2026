package mexa.club.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.notificationservice.dto.ApiResponse;
import mexa.club.notificationservice.dto.NotificationResponse;
import mexa.club.notificationservice.exception.NotificationException;
import mexa.club.notificationservice.security.JwtUserPrincipal;
import mexa.club.notificationservice.service.NotificationMapper;
import mexa.club.notificationservice.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Tag(name = "My Notifications", description = "Endpoints for the authenticated user to view their own notifications")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
public class MyNotificationController {
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    public MyNotificationController(NotificationService notificationService, NotificationMapper notificationMapper) {
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }

    @Operation(
        summary = "Get my notifications",
        description = "Returns a paginated list of notifications for the currently authenticated user, ordered by creation date descending."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "NOTIFICATION_VIEW authority required")
    })
    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> my(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of notifications per page")
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        UUID userId = userId(authentication);
        var result = notificationService.my(userId, page, size);
        return ApiResponse.ok(Map.of(
                "content", result.getContent().stream().map(notificationMapper::toResponse).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        ));
    }

    @Operation(
            summary = "Unread notifications count",
            description = "Returns the number of unread notifications for the currently authenticated user."
    )
    @GetMapping("/my/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount(Authentication authentication) {
        UUID userId = userId(authentication);
        return ApiResponse.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    @Operation(
            summary = "Mark one notification as read",
            description = "Marks a single notification (owned by the current user) as read."
    )
    @PatchMapping("/my/{id}/read")
    public ApiResponse<Map<String, Object>> markRead(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = userId(authentication);
        boolean updated = notificationService.markRead(id, userId);
        return ApiResponse.ok(Map.of("updated", updated));
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks all notifications of the current user as read."
    )
    @PatchMapping("/my/read-all")
    public ApiResponse<Map<String, Object>> markAllRead(Authentication authentication) {
        UUID userId = userId(authentication);
        int updated = notificationService.markAllRead(userId);
        return ApiResponse.ok(Map.of("updated", updated));
    }

    private UUID userId(Authentication authentication) {
        Object p = authentication.getPrincipal();
        if (p instanceof JwtUserPrincipal principal) {
            return principal.userId();
        }
        throw new NotificationException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid principal");
    }
}
