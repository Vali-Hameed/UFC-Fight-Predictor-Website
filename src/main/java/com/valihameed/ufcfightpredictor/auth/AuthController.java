package com.valihameed.ufcfightpredictor.auth;

import com.valihameed.ufcfightpredictor.security.JwtService;
import com.valihameed.ufcfightpredictor.security.RefreshTokenService;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.users.user;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.Duration;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final userRepository userRepository;
    @org.springframework.beans.factory.annotation.Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        user u = (user) auth.getPrincipal();
        String accessToken = jwtService.generateToken(u.getUsername());
        String refreshRaw = refreshTokenService.createRefreshToken(u.getId());
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshRaw)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(Duration.ofDays(7))
            .sameSite("Lax")
            .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(new AuthResponse(accessToken, 15 * 60));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.findByRaw(refreshToken).ifPresent(refreshTokenObj -> refreshTokenService.revoke(refreshTokenObj));
        }
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "").httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite("Lax").build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        var refreshTokenEntity = refreshTokenService.findByRaw(refreshToken).orElse(null);
        if (refreshTokenEntity == null || refreshTokenEntity.getRevoked() || refreshTokenEntity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return ResponseEntity.status(401).build();
        }

        Long userId = refreshTokenEntity.getUserId();
        user u = userRepository.findById(userId).orElse(null);
        if (u == null) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = jwtService.generateToken(u.getUsername());
        refreshTokenService.revoke(refreshTokenEntity);
        String newRaw = refreshTokenService.createRefreshToken(userId);
        long maxAgeSecs = Duration.ofDays(7).getSeconds();
        ResponseCookie cookie = ResponseCookie.from("refresh_token", newRaw)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(maxAgeSecs)
            .sameSite("Lax")
            .build();
        AuthResponse response = new AuthResponse(accessToken, 15 * 60);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response);
    }
}
