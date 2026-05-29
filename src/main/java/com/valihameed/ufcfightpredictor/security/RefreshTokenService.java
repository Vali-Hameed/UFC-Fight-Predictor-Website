package com.valihameed.ufcfightpredictor.security;

import com.valihameed.ufcfightpredictor.models.RefreshToken;
import com.valihameed.ufcfightpredictor.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiry-days:7}")
    private int refreshExpiryDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String createRefreshToken(Long userId) {
        String raw = UUID.randomUUID().toString();
        String hash = hash(raw);
        OffsetDateTime now = OffsetDateTime.now();
        RefreshToken token = RefreshToken.builder()
            .userId(userId)
            .tokenHash(hash)
            .expiresAt(now.plusDays(refreshExpiryDays))
            .revoked(false)
            .createdAt(now)
            .build();
        refreshTokenRepository.save(token);
        return raw;
    }

    public Optional<RefreshToken> findByRaw(String raw) {
        String hash = hash(raw);
        return refreshTokenRepository.findByTokenHash(hash);
    }

    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] h = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(h);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
