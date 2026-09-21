package mexa.club.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.authservice.api.ApiResponse;
import mexa.club.authservice.dto.InternalAuthStatsResponse;
import mexa.club.authservice.dto.InternalUserGrowthPoint;
import mexa.club.authservice.dto.InternalUserProfileResponse;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.repository.UserRepository;
import mexa.club.authservice.service.InternalAuthAnalyticsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Internal — Auth Analytics", description = "Internal service-to-service endpoints for auth analytics. All requests must include the X-Internal-Secret header matching the configured secret. These endpoints are not intended to be exposed publicly.")
@RestController
@RequestMapping("/internal/auth")
public class InternalUserController {

    private final InternalAuthAnalyticsService internalAuthAnalyticsService;
    private final UserRepository userRepository;
    private final String internalSecret;

    public InternalUserController(
            InternalAuthAnalyticsService internalAuthAnalyticsService,
            UserRepository userRepository,
            @Value("${app.internal-secret}") String internalSecret
    ) {
        this.internalAuthAnalyticsService = internalAuthAnalyticsService;
        this.userRepository = userRepository;
        this.internalSecret = internalSecret;
    }

    @Operation(
            summary = "Get auth statistics for a time range",
            description = "Returns aggregate authentication statistics (e.g. total registrations, logins, failures) for the specified datetime range. Caller must supply the X-Internal-Secret header."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid 'from' / 'to' query parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or does not match")
    })
    @SecurityRequirements
    @GetMapping("/stats")
    public ApiResponse<InternalAuthStatsResponse> stats(
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true)
            @RequestHeader("X-Internal-Secret") String secret,
            @Parameter(description = "Start of the time range (ISO-8601 datetime, e.g. 2025-01-01T00:00:00)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "End of the time range (ISO-8601 datetime, e.g. 2025-12-31T23:59:59)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        ensureSecret(secret);
        return ApiResponse.ok(internalAuthAnalyticsService.stats(from, to));
    }

    @Operation(
            summary = "Get user registration growth data",
            description = "Returns a time-series list of user registration counts grouped by day for the given date range. Intended for dashboard analytics. Caller must supply the X-Internal-Secret header."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Growth data returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid 'from' / 'to' query parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or does not match")
    })
    @SecurityRequirements
    @GetMapping("/users/growth")
    public ApiResponse<List<InternalUserGrowthPoint>> growth(
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true)
            @RequestHeader("X-Internal-Secret") String secret,
            @Parameter(description = "Start date (ISO-8601 date, e.g. 2025-01-01)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (ISO-8601 date, e.g. 2025-12-31)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        ensureSecret(secret);
        return ApiResponse.ok(internalAuthAnalyticsService.growth(from, to));
    }

    @Operation(
            summary = "Resolve user emails by IDs",
            description = "Returns a map of userId -> email for the requested user IDs. Intended for internal services that need recipient addresses (e.g. warehouse low-stock notifications). Caller must supply the X-Internal-Secret header."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Emails resolved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid 'ids' query parameter"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or does not match")
    })
    @SecurityRequirements
    @GetMapping("/users/by-ids")
    public ApiResponse<Map<UUID, String>> resolveEmails(
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true)
            @RequestHeader("X-Internal-Secret") String secret,
            @Parameter(description = "Comma-separated list of user UUIDs", required = true)
            @RequestParam("ids") List<UUID> ids
    ) {
        ensureSecret(secret);
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids must not be empty");
        }
        Map<UUID, String> emails = new LinkedHashMap<>();
        for (User user : userRepository.findAllById(ids)) {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                emails.put(user.getId(), user.getEmail());
            }
        }
        return ApiResponse.ok(emails);
    }

    @Operation(
            summary = "Get user profile by ID (internal)",
            description = "Returns the basic profile (id, username, email) of the specified user. Used by other services (e.g. order-service resolving the recipient name when creating a delivery). Caller must supply the X-Internal-Secret header."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "X-Internal-Secret header is missing or does not match"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirements
    @GetMapping("/users/{id}/profile")
    public ApiResponse<InternalUserProfileResponse> userProfile(
            @Parameter(description = "UUID of the user to resolve", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Shared internal secret for service-to-service authentication", required = true)
            @RequestHeader("X-Internal-Secret") String secret
    ) {
        ensureSecret(secret);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ApiResponse.ok(new InternalUserProfileResponse(user.getId(), user.getUsername(), user.getEmail()));
    }

    private void ensureSecret(String secret) {
        if (secret == null || !secret.equals(internalSecret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal secret");
        }
    }
}
