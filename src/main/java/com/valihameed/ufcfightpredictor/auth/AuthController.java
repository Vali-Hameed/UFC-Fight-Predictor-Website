package com.valihameed.ufcfightpredictor.auth;

import com.valihameed.ufcfightpredictor.security.JwtService;
import com.valihameed.ufcfightpredictor.security.RefreshTokenService;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.users.user;
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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        user u = (user) auth.getPrincipal();
        String accessToken = jwtService.generateToken(u.getUsername());
        String refreshRaw = refreshTokenService.createRefreshToken(u.getId());
        // TODO: link refresh token to actual user id when user model available; placeholder
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshRaw)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(new AuthResponse(accessToken, 15 * 60));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.findByRaw(refreshToken).ifPresent(refreshTokenObj -> refreshTokenService.revoke(refreshTokenObj));
        }
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "").httpOnly(true).path("/").maxAge(0).build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        return refreshTokenService.findByRaw(refreshToken).map(rt -> {
            if (rt.getRevoked() || rt.getExpiresAt().isBefore(OffsetDateTime.now())) {
                return ResponseEntity.status(401).build();
            }
            Long userId = rt.getUserId();
            return userRepository.findById(userId).map(u -> {
                String username = u.getUsername();
                String accessToken = jwtService.generateToken(username);
                // rotate refresh token: revoke old, create new
                refreshTokenService.revoke(rt);
                String newRaw = refreshTokenService.createRefreshToken(userId);
                long maxAgeSecs = java.time.Duration.between(OffsetDateTime.now(), OffsetDateTime.now().plusDays(7)).getSeconds();
                ResponseCookie cookie = ResponseCookie.from("refresh_token", newRaw)
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .maxAge(maxAgeSecs)
                        .sameSite("Lax")
                        .build();
                return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(new AuthResponse(accessToken, 15 * 60));
            }).orElse(ResponseEntity.status(401).build());
        }).orElse(ResponseEntity.status(401).build());
    }
}
