package mexa.club.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.notificationservice.dto.ApiResponse;
import mexa.club.notificationservice.dto.NotificationResponse;
import mexa.club.notificationservice.dto.TemplateUpdateRequest;
import mexa.club.notificationservice.entity.Notification;
import mexa.club.notificationservice.entity.NotificationTemplate;
import mexa.club.notificationservice.service.NotificationMapper;
import mexa.club.notificationservice.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Admin - Notifications", description = "Admin endpoints for viewing, resending notifications and managing notification templates")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('NOTIFICATION_MANAGE')")
public class AdminNotificationController {
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    public AdminNotificationController(NotificationService notificationService, NotificationMapper notificationMapper) {
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }

    @Operation(
        summary = "List all notifications",
        description = "Returns a paginated list of all notifications in the system across all users, ordered by creation date descending. Requires NOTIFICATION_MANAGE authority."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "NOTIFICATION_MANAGE authority required")
    })
    @GetMapping("/notifications")
    public ApiResponse<Map<String, Object>> all(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of notifications per page")
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationResponse> result = notificationService.all(page, size).map(notificationMapper::toResponse);
        return ApiResponse.ok(Map.of(
                "content", result.getContent(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        ));
    }

    @Operation(
        summary = "Get notification details",
        description = "Returns the full details of a single notification by its UUID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "NOTIFICATION_MANAGE authority required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @GetMapping("/notifications/{id}")
    public ApiResponse<NotificationResponse> detail(
            @Parameter(description = "UUID of the notification", required = true)
            @PathVariable UUID id) {
        Notification n = notificationService.get(id);
        return ApiResponse.ok(notificationMapper.toResponse(n));
    }

    @Operation(
        summary = "Resend a notification",
        description = "Re-enqueues the specified notification for delivery. Useful when the original delivery failed."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification resent successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "NOTIFICATION_MANAGE authority required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PostMapping("/notifications/resend/{id}")
    public ApiResponse<NotificationResponse> resend(
            @Parameter(description = "UUID of the notification to resend", required = true)
            @PathVariable UUID id) {
        return ApiResponse.ok(notificationMapper.toResponse(notificationService.resend(id)));
    }

    @Operation(
        summary = "List notification templates",
        description = "Returns all notification templates used for rendering notification messages."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Templates retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "NOTIFICATION_MANAGE authority required")
    })
    @GetMapping("/templates")
    public ApiResponse<java.util.List<NotificationTemplate>> templates() {
        return ApiResponse.ok(notificationService.templates());
    }

    @Operation(
        summary = "Update a notification template",
        description = "Updates the subject and body of the specified notification template."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Template updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid template data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "NOTIFICATION_MANAGE authority required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found")
    })
    @PutMapping("/templates/{id}")
    public ApiResponse<NotificationTemplate> updateTemplate(
            @Parameter(description = "UUID of the template to update", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody TemplateUpdateRequest request) {
        return ApiResponse.ok(notificationService.updateTemplate(id, request));
    }
}
