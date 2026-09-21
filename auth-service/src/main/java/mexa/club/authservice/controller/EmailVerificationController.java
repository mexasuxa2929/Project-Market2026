package mexa.club.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import mexa.club.authservice.dto.VerifyEmailRequest;
import mexa.club.authservice.service.EmailVerificationService;
import mexa.club.authservice.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Email Verification", description = "Endpoints for verifying user email addresses using a 6-digit OTP code. Codes are generated with SecureRandom and stored server-side as a SHA-256 hash.")
@RestController
@RequestMapping("/api/auth")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService,
            RefreshTokenService refreshTokenService
    ) {
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
    }

    @Operation(
            summary = "Verify email address with OTP",
            description = "Completes user registration by verifying the 6-digit OTP code sent to the user's email during /api/auth/register. On success, the user record is moved to the main users table and a JWT access token plus refresh token are returned (auto-login)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully; access and refresh tokens returned for immediate login"),
            @ApiResponse(responseCode = "400", description = "OTP code is invalid, expired, or maximum verification attempts exceeded"),
            @ApiResponse(responseCode = "404", description = "No pending registration found for the given email")
    })
    @SecurityRequirements
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest servletRequest
    ) {
        // 1. OTP ni tekshir va userni yaratadi
        String username = emailVerificationService.verifyEmail(request.getEmail(), request.getOtpCode());

        // 2. Darhol token beradi (auto-login)
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.issueForUsername(
                username,
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr(),
                null
        );

        Map<String, Object> data = new HashMap<>();
        data.put("token", tokenPair.accessToken());
        data.put("refreshToken", tokenPair.refreshToken());
        data.put("sessionId", tokenPair.sessionId());
        data.put("expiresIn", tokenPair.expiresInSeconds());
        data.put("tokenType", tokenPair.tokenType());

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", data);
        return ResponseEntity.ok(body);
    }
}
