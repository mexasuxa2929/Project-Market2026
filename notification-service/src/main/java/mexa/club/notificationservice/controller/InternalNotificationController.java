package mexa.club.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.notificationservice.dto.ApiResponse;
import mexa.club.notificationservice.dto.BulkNotificationRequest;
import mexa.club.notificationservice.dto.NotificationResponse;
import mexa.club.notificationservice.dto.SendNotificationRequest;
import mexa.club.notificationservice.exception.NotificationException;
import mexa.club.notificationservice.service.NotificationMapper;
import mexa.club.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Internal - Notifications", description = "Internal endpoints for sending notifications from other microservices. Protected by X-Internal-Secret header.")
@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final String internalSecret;

    public InternalNotificationController(
            NotificationService notificationService,
            NotificationMapper notificationMapper,
            @Value("${app.internal-secret}") String internalSecret
    ) {
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
        this.internalSecret = internalSecret;
    }

    @Operation(
            summary = "Enqueue a single notification",
            description = "Queues a single notification for delivery to a user. Called by other microservices (e.g. order-service). Requires the X-Internal-Secret header matching the configured app.internal-secret value."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification enqueued successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid notification request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Invalid or missing X-Internal-Secret header")
    })
    @PostMapping("/send")
    public ApiResponse<NotificationResponse> send(
            @Parameter(description = "Internal service secret key matching app.internal-secret", required = true)
            @RequestHeader("X-Internal-Secret") String secret,
            @Valid @RequestBody SendNotificationRequest request
    ) {
        ensureSecret(secret);
        return ApiResponse.ok(notificationMapper.toResponse(notificationService.enqueue(request)));
    }

    @Operation(summary = "Enqueue bulk notifications",
        description = "Queues multiple notifications for delivery in a single request. Useful for broadcast scenarios such as promotional messages or system alerts.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bulk notifications enqueued successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid bulk notification request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Invalid or missing X-Internal-Secret header")
    })
    @PostMapping("/send-bulk")
    public ApiResponse<List<NotificationResponse>> sendBulk(
            @Parameter(description = "Internal service secret key matching app.internal-secret", required = true)
            @RequestHeader("X-Internal-Secret") String secret,
            @Valid @RequestBody BulkNotificationRequest request
    ) {
        ensureSecret(secret);
        return ApiResponse.ok(notificationService.enqueueBulk(request).stream().map(notificationMapper::toResponse).toList());
    }

    private void ensureSecret(String secret) {
        if (secret == null || !secret.equals(internalSecret)) {
            throw new NotificationException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Invalid internal secret");
        }
    }
}
