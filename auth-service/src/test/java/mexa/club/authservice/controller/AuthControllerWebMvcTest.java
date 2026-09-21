package mexa.club.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mexa.club.authservice.config.JwtSessionProperties;
import mexa.club.authservice.config.RefreshTokenProperties;
import mexa.club.authservice.dto.LoginRequest;
import mexa.club.authservice.dto.RefreshTokenRequest;
import mexa.club.authservice.dto.ResendCodeRequest;
import mexa.club.authservice.security.JwtRequestFilter;
import mexa.club.authservice.security.JwtUtil;
import mexa.club.authservice.security.SwaggerAccessFilter;
import mexa.club.authservice.service.EmailVerificationService;
import mexa.club.authservice.service.GoogleAuthService;
import mexa.club.authservice.service.PasswordResetService;
import mexa.club.authservice.service.RefreshTokenService;
import mexa.club.authservice.service.TokenBlacklistService;
import mexa.club.authservice.service.UserSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private mexa.club.authservice.repository.UserRepository userRepository;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private EmailVerificationService emailVerificationService;
    @MockBean
    private RefreshTokenService refreshTokenService;
    @MockBean
    private RefreshTokenProperties refreshTokenProperties;
    @MockBean
    private PasswordResetService passwordResetService;
    @MockBean
    private GoogleAuthService googleAuthService;
    @MockBean
    private UserSessionService userSessionService;
    @MockBean
    private TokenBlacklistService tokenBlacklistService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private JwtSessionProperties jwtSessionProperties;
    @MockBean
    private JwtRequestFilter jwtRequestFilter;
    @MockBean
    private SwaggerAccessFilter swaggerAccessFilter;

    @Test
    void login_returnsAccessAndRefreshTokens_andSetsCookie() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("password");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(refreshTokenService.issueForUsername(eq("admin"), any(), any(), any()))
                .thenReturn(new RefreshTokenService.TokenPair("access-token", "refresh-token", UUID.randomUUID(), 900L, "Bearer"));
        when(refreshTokenProperties.getCookie()).thenReturn(cookieProps(true));
        when(jwtSessionProperties.getRefreshTokenTtl()).thenReturn(1_209_600L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    void refresh_withBodyToken_returnsNewRotatedTokens() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("old-refresh");

        when(refreshTokenService.rotate(eq("old-refresh"), any(), any(), any()))
                .thenReturn(new RefreshTokenService.TokenPair("new-access", "new-refresh", UUID.randomUUID(), 900L, "Bearer"));
        when(refreshTokenProperties.getCookie()).thenReturn(cookieProps(true));
        when(jwtSessionProperties.getRefreshTokenTtl()).thenReturn(1_209_600L);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    void resendCode_withValidEmail_returnsOk() throws Exception {
        ResendCodeRequest req = new ResendCodeRequest();
        req.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/resend-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void resendCode_withInvalidEmail_returnsBadRequest() throws Exception {
        ResendCodeRequest req = new ResendCodeRequest();
        req.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/resend-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    private static RefreshTokenProperties.Cookie cookieProps(boolean enabled) {
        RefreshTokenProperties.Cookie cookie = new RefreshTokenProperties.Cookie();
        cookie.setEnabled(enabled);
        cookie.setName("refreshToken");
        cookie.setPath("/api/auth");
        cookie.setSameSite("Lax");
        cookie.setSecure(false);
        return cookie;
    }
}
