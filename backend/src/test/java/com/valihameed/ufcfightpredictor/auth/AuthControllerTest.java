package com.valihameed.ufcfightpredictor.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valihameed.ufcfightpredictor.models.RefreshToken;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.security.JwtService;
import com.valihameed.ufcfightpredictor.security.RefreshTokenService;
import com.valihameed.ufcfightpredictor.users.user;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private JwtService jwtService;
    @MockBean private RefreshTokenService refreshTokenService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private userRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private user testUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        testUser = new user();
        testUser.setId(1L);
        testUser.setUsername("johndoe");
        testUser.setTokenVersion(1);

        authentication = new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList());
    }

    @Test
    void canLoginAndReceiveTokens() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("johndoe");
        request.setPassword("password123");

        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtService.generateToken("johndoe", 1)).willReturn("access-token-123");
        given(refreshTokenService.createRefreshToken(1L)).willReturn("refresh-token-123");

        // When / Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().value("refresh_token", "refresh-token-123"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void canLogoutAndRevokeToken() throws Exception {
        // Given
        RefreshToken rt = new RefreshToken();
        rt.setId(1L);
        rt.setTokenHash("hashed");

        given(refreshTokenService.findByRaw("valid-refresh-token")).willReturn(Optional.of(rt));

        // When / Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value("refresh_token", ""))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void canRefreshAccessToken() throws Exception {
        // Given
        RefreshToken rt = new RefreshToken();
        rt.setId(1L);
        rt.setUserId(1L);
        rt.setTokenHash("hashed");
        rt.setRevoked(false);
        rt.setExpiresAt(OffsetDateTime.now().plusDays(1));

        given(refreshTokenService.findByRaw("valid-refresh-token")).willReturn(Optional.of(rt));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(jwtService.generateToken("johndoe", 1)).willReturn("new-access-token");
        given(refreshTokenService.createRefreshToken(1L)).willReturn("new-refresh-token");

        // When / Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(cookie().value("refresh_token", "new-refresh-token"));
    }

    @Test
    void refreshFailsIfTokenIsRevoked() throws Exception {
        // Given
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(true);
        rt.setExpiresAt(OffsetDateTime.now().plusDays(1));

        given(refreshTokenService.findByRaw("revoked-token")).willReturn(Optional.of(rt));

        // When / Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", "revoked-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshFailsIfTokenIsExpired() throws Exception {
        // Given
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(false);
        rt.setExpiresAt(OffsetDateTime.now().minusDays(1)); // Expired

        given(refreshTokenService.findByRaw("expired-token")).willReturn(Optional.of(rt));

        // When / Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", "expired-token")))
                .andExpect(status().isUnauthorized());
    }
}
