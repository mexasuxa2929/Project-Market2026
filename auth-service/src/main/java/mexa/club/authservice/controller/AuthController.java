package mexa.club.authservice.controller;

import mexa.club.authservice.dto.LoginRequest;
import mexa.club.authservice.dto.ForgotPasswordRequest;
import mexa.club.authservice.dto.GoogleAuthRequest;
import mexa.club.authservice.dto.RefreshTokenRequest;
import mexa.club.authservice.dto.RegisterRequest;
import mexa.club.authservice.dto.ResendCodeRequest;
import mexa.club.authservice.dto.ResetPasswordRequest;
import mexa.club.authservice.dto.VerifyResetCodeRequest;
import mexa.club.authservice.dto.UserSessionResponse;
import mexa.club.authservice.config.JwtSessionProperties;
import mexa.club.authservice.security.JwtUtil;
import mexa.club.authservice.config.RefreshTokenProperties;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.repository.UserRepository;
import mexa.club.authservice.service.EmailVerificationService;
import mexa.club.authservice.service.GoogleAuthService;
import mexa.club.authservice.service.PasswordResetService;
import mexa.club.authservice.service.RefreshTokenService;
import mexa.club.authservice.service.TokenBlacklistService;
import mexa.club.authservice.service.UserSessionService;
import mexa.club.authservice.service.LoginAttemptService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;
import java.util.UUID;

@Tag(name = "Auth", description = "Authentication endpoints for login, registration, token refresh, logout, and password reset. Most endpoints are public and do not require a JWT token.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenProperties refreshTokenProperties;
    private final PasswordResetService passwordResetService;
    private final GoogleAuthService googleAuthService;
    private final UserSessionService userSessionService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;
    private final JwtSessionProperties jwtSessionProperties;
    private final LoginAttemptService loginAttemptService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            RefreshTokenService refreshTokenService,
            RefreshTokenProperties refreshTokenProperties,
            PasswordResetService passwordResetService,
            GoogleAuthService googleAuthService,
            UserSessionService userSessionService,
            TokenBlacklistService tokenBlacklistService,
            JwtUtil jwtUtil,
            JwtSessionProperties jwtSessionProperties,
            LoginAttemptService loginAttemptService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenProperties = refreshTokenProperties;
        this.passwordResetService = passwordResetService;
        this.googleAuthService = googleAuthService;
        this.userSessionService = userSessionService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtUtil = jwtUtil;
        this.jwtSessionProperties = jwtSessionProperties;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login with username or email",
            description = "Authenticates a user with their username (or email) and password. Returns a JWT access token and a refresh token on success."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated; returns access token, refresh token, session ID, and expiry"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials — wrong username/email or password"),
            @ApiResponse(responseCode = "429", description = "Too many failed login attempts — account temporarily locked")
    })
    @SecurityRequirements
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        String username = loginRequest.getUsernameOrEmail();

        // Brute force himoya: bloklangan foydalanuvchi tekshirish
        if (loginAttemptService.isLocked(username)) {
            long remaining = loginAttemptService.getRemainingLockoutSeconds(username);
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("error", Map.of(
                    "code", "ACCOUNT_LOCKED",
                    "message", "Juda ko'p noto'g'ri urinish. " + (remaining / 60) + " daqiqadan keyin qayta urinib ko'ring.",
                    "retryAfterSeconds", remaining
            ));
            return ResponseEntity.status(429).body(errorBody);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));

            // Muvaffaqiyatli kirish — urinishlarni tozalash
            loginAttemptService.resetAttempts(username);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            RefreshTokenService.TokenPair tokenPair =
                    refreshTokenService.issueForUsername(
                            authentication.getName(),
                            servletRequest.getHeader("User-Agent"),
                            servletRequest.getRemoteAddr(),
                            loginRequest.getDeviceId()
                    );
            addRefreshCookieIfEnabled(response, tokenPair.refreshToken());
            return ResponseEntity.ok(tokenBody(tokenPair));
        } catch (Exception e) {
            // Noto'g'ri urinish — qayd etish
            loginAttemptService.recordFailedAttempt(username);
            throw e;
        }
    }

    @PostMapping("/google")
    @Operation(
            summary = "Google OAuth2 login or register",
            description = "Verifies a Google ID token received from the frontend. If the Google account is new, a user is automatically created. Returns a JWT access token and refresh token.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = GoogleAuthRequest.class))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated or registered via Google; returns access and refresh tokens"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed request body"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired Google ID token")
    })
    @SecurityRequirements
    public ResponseEntity<Map<String, Object>> googleAuth(
            @Valid @RequestBody GoogleAuthRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        RefreshTokenService.TokenPair tokenPair = googleAuthService.authenticate(
                request.getIdToken(),
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr(),
                request.getDeviceId()
        );
        addRefreshCookieIfEnabled(response, tokenPair.refreshToken());
        return ResponseEntity.ok(tokenBody(tokenPair));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Issues a new access token and refresh token using a valid refresh token (token rotation). The refresh token can be supplied in the request body or via the HttpOnly cookie set during login."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access and refresh tokens issued successfully"),
            @ApiResponse(responseCode = "400", description = "Refresh token is missing or blank"),
            @ApiResponse(responseCode = "401", description = "Refresh token is invalid, expired, or already revoked")
    })
    @SecurityRequirements
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        String rawRefreshToken = resolveRefreshToken(request, servletRequest);
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotate(
                rawRefreshToken,
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr(),
                request != null ? request.getDeviceId() : null
        );
        addRefreshCookieIfEnabled(response, tokenPair.refreshToken());
        return ResponseEntity.ok(tokenBody(tokenPair));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout current session",
            description = "Revokes the refresh token and blacklists the current access token so it can no longer be used. Also clears the refresh-token HttpOnly cookie if cookies are enabled."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully logged out"),
            @ApiResponse(responseCode = "400", description = "Refresh token is missing or blank")
    })
    @SecurityRequirements
    public ResponseEntity<Map<String, Object>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        String accessToken = resolveBearerToken(servletRequest);
        if (accessToken != null) {
            long remainingTtl = jwtUtil.remainingValiditySeconds(accessToken);
            tokenBlacklistService.blacklist(accessToken, remainingTtl);
        }
        String rawRefreshToken = resolveRefreshToken(request, servletRequest);
        refreshTokenService.revoke(rawRefreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("loggedOut", true)));
    }

    @GetMapping("/sessions")
    @Operation(
            summary = "List active sessions",
            description = "Returns all active sessions for the currently authenticated user. The session matching the current refresh cookie is marked as the current session."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of active sessions returned successfully"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<UserSessionResponse>> sessions(HttpServletRequest request) {
        UUID userId = resolveCurrentUserId();
        UUID currentSessionId = resolveSessionIdFromRefreshCookie(request);
        return ResponseEntity.ok(userSessionService.listActiveByUser(userId, currentSessionId));
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(
            summary = "Close a specific session",
            description = "Closes (revokes) a single active session by its UUID. Only sessions belonging to the currently authenticated user can be closed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session closed successfully"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "The session does not belong to the current user"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> closeSession(
            @Parameter(description = "UUID of the session to close", required = true)
            @PathVariable UUID id) {
        UUID userId = resolveCurrentUserId();
        userSessionService.closeOne(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("closed", true)));
    }

    @DeleteMapping("/sessions")
    @Operation(
            summary = "Close all sessions",
            description = "Revokes all active sessions for the currently authenticated user, effectively logging out from every device."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All sessions closed successfully"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> closeAllSessions() {
        UUID userId = resolveCurrentUserId();
        userSessionService.closeAll(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("closedAll", true)));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = "Accepts an email address and sends a 6-digit password reset code to that address if an account exists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset code sent if the account exists"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid email in request body"),
            @ApiResponse(responseCode = "404", description = "Email not found in system")
    })
    @SecurityRequirements
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        boolean sent = passwordResetService.forgotPassword(request.getEmail());
        if (!sent) {
            Map<String, Object> err = Map.of("code", "EMAIL_NOT_FOUND", "message", "Bu email tizimda topilmadi");
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("success", false);
            body.put("error", err);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("message", "Reset code sent")
        ));
    }

    @PostMapping("/verify-reset-code")
    @Operation(
            summary = "Verify password reset code",
            description = "Validates the 6-digit code sent to the user's email. Must be called before submitting a new password via /reset-password."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code is valid and verified"),
            @ApiResponse(responseCode = "400", description = "Code is invalid, expired, or maximum attempts exceeded")
    })
    @SecurityRequirements
    public ResponseEntity<Map<String, Object>> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        passwordResetService.verifyResetCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("verified", true)
        ));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password",
            description = "Sets a new password using the email address, a previously verified 6-digit reset code, and the desired new password. The code must have been verified via /verify-reset-code first."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Code is invalid, expired, or new password does not meet requirements")
    })
    @SecurityRequirements
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("reset", true)
        ));
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Initiates user registration by sending a 6-digit OTP code to the provided email. "
                    + "User data is stored temporarily until email verification is completed via /api/auth/verify-email. "
                    + "If the same email is re-submitted, the previous pending entry is replaced and a new code is sent. "
                    + "Login is only possible after successful email verification."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "OTP sent to email — complete registration via /api/auth/verify-email"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields"),
            @ApiResponse(responseCode = "409", description = "Email is already in use by a verified account, or username is already taken")
    })
    @SecurityRequirements
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        String normalizedEmail = registerRequest.getEmail() == null ? null : registerRequest.getEmail().trim().toLowerCase();

        Optional<User> existingByEmail = userRepository.findByEmail(normalizedEmail);
        if (existingByEmail.isPresent()) {
            User u = existingByEmail.get();
            if (u.isVerified()) {
                return conflict("EMAIL_IN_USE", "Email is already in use");
            }
            userRepository.delete(u);
        }

        if (emailVerificationService.isUsernameBlockedForRegistration(registerRequest.getUsername(), normalizedEmail)) {
            return conflict("USERNAME_TAKEN", "Username is already taken");
        }

        String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());
        emailVerificationService.createAndSendRegistrationOtp(
                normalizedEmail,
                registerRequest.getUsername(),
                encodedPassword
        );

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", Map.of("message", "Verification code sent to email. Complete registration with /api/auth/verify-email."));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/resend-code")
    @Operation(
            summary = "Resend OTP verification code",
            description = "Generates and sends a new 6-digit OTP code to the given email. The previous code is invalidated. A minimum resend interval (default 60 seconds) is enforced to prevent abuse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New verification code sent to email"),
            @ApiResponse(responseCode = "400", description = "Email not found in pending registrations, or resend interval not elapsed yet")
    })
    @SecurityRequirements
    public ResponseEntity<Map<String, Object>> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        emailVerificationService.resendVerificationOtp(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("message", "Verification code resent")
        ));
    }

    private static ResponseEntity<Map<String, Object>> conflict(String code, String message) {
        Map<String, Object> err = Map.of("code", code, "message", message);
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", err);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            return request.getRefreshToken().trim();
        }
        String cookieName = refreshTokenProperties.getCookie().getName();
        Cookie[] cookies = servletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie != null && cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue().trim();
                }
            }
        }
        throw new IllegalArgumentException("refreshToken is required");
    }

    private void addRefreshCookieIfEnabled(HttpServletResponse response, String refreshToken) {
        if (!refreshTokenProperties.getCookie().isEnabled()) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(refreshTokenProperties.getCookie().getName(), refreshToken)
                .httpOnly(true)
                .secure(refreshTokenProperties.getCookie().isSecure())
                .path(refreshTokenProperties.getCookie().getPath())
                .sameSite(refreshTokenProperties.getCookie().getSameSite())
                .maxAge(Duration.ofSeconds(jwtSessionProperties.getRefreshTokenTtl()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        if (!refreshTokenProperties.getCookie().isEnabled()) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(refreshTokenProperties.getCookie().getName(), "")
                .httpOnly(true)
                .secure(refreshTokenProperties.getCookie().isSecure())
                .path(refreshTokenProperties.getCookie().getPath())
                .sameSite(refreshTokenProperties.getCookie().getSameSite())
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static Map<String, Object> tokenBody(RefreshTokenService.TokenPair tokenPair) {
        Map<String, Object> data = new HashMap<>();
        data.put("token", tokenPair.accessToken());
        data.put("refreshToken", tokenPair.refreshToken());
        data.put("sessionId", tokenPair.sessionId());
        data.put("expiresIn", tokenPair.expiresInSeconds());
        data.put("tokenType", tokenPair.tokenType());
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", data);
        return body;
    }

    private UUID resolveCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Not authenticated"));
        return user.getId();
    }

    private UUID resolveSessionIdFromRefreshCookie(HttpServletRequest request) {
        try {
            String refreshToken = resolveRefreshToken(null, request);
            return refreshTokenService.resolveSessionId(refreshToken);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolveBearerToken(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        String token = auth.substring(7).trim();
        return token.isBlank() ? null : token;
    }
}
