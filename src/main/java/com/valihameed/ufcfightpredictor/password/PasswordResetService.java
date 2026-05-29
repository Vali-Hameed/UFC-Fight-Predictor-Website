package com.valihameed.ufcfightpredictor.password;

import com.valihameed.ufcfightpredictor.email.EmailSender;
import com.valihameed.ufcfightpredictor.models.PasswordResetToken;
import com.valihameed.ufcfightpredictor.models.user;
import com.valihameed.ufcfightpredictor.repository.PasswordResetTokenRepository;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PasswordResetService {
    private final PasswordResetTokenRepository tokenRepository;
    private final userRepository userRepository;
    private final EmailSender emailSender;
    private final BCryptPasswordEncoder passwordEncoder;

    public String createPasswordReset(String email) {
        user u = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        String raw = UUID.randomUUID().toString();
        String hash = hash(raw);
        PasswordResetToken token = PasswordResetToken.builder().userId(u.getId()).tokenHash(hash).expiresAt(OffsetDateTime.now().plusMinutes(30)).used(false).createdAt(OffsetDateTime.now()).build();
        tokenRepository.save(token);
        String link = String.format("http://localhost:3000/reset-password?token=%s", raw);
        emailSender.sendEmail(u.getEmail(), String.format("Click to reset: %s", link));
        return raw;
    }

    public void confirmPasswordReset(String rawToken, String newPassword) {
        String hash = hash(rawToken);
        PasswordResetToken token = tokenRepository.findByTokenHash(hash).orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (token.getUsed() != null && token.getUsed()) throw new IllegalArgumentException("Token already used");
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) throw new IllegalArgumentException("Token expired");
        user u = userRepository.findById(token.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        token.setUsed(true);
        tokenRepository.save(token);
    }

    private String hash(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(h);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
