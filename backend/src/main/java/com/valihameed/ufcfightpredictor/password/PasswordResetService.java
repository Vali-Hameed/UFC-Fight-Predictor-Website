package com.valihameed.ufcfightpredictor.password;

import com.valihameed.ufcfightpredictor.email.EmailSender;
import com.valihameed.ufcfightpredictor.models.PasswordResetToken;
import com.valihameed.ufcfightpredictor.repository.PasswordResetTokenRepository;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.users.user;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.valihameed.ufcfightpredictor.security.RefreshTokenService;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final PasswordResetTokenRepository tokenRepository;
    private final userRepository userRepository;
    private final EmailSender emailSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public String createPasswordReset(String email) {
        user u = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<PasswordResetToken> oldTokens = tokenRepository.findByUserIdAndUsedFalse(u.getId());
        for (PasswordResetToken oldToken : oldTokens) {
            oldToken.setUsed(true);
        }
        tokenRepository.saveAll(oldTokens);

        String raw = UUID.randomUUID().toString();
        String hash = hash(raw);
        PasswordResetToken token = PasswordResetToken.builder().userId(u.getId()).tokenHash(hash).expiresAt(OffsetDateTime.now().plusMinutes(15)).used(false).createdAt(OffsetDateTime.now()).build();
        tokenRepository.save(token);
        String link = String.format("%s/reset-password?token=%s", frontendUrl, raw);
        emailSender.sendEmail(u.getEmail(), buildEmail(u.getFirstName() != null ? u.getFirstName() : u.getUsername(), link), "Reset your password");
        return raw;
    }

    private String buildEmail(String name, String link) {
        return "<div style=\"font-family: Arial, sans-serif; background-color: #09090b; color: #ffffff; padding: 40px 20px; text-align: center;\">\n" +
               "  <div style=\"max-width: 600px; margin: 0 auto; background-color: #121212; border: 1px solid #333; border-radius: 8px; overflow: hidden;\">\n" +
               "    <div style=\"background-color: #dc2626; padding: 20px;\">\n" +
               "      <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">FightPicks</h1>\n" +
               "    </div>\n" +
               "    <div style=\"padding: 30px 20px;\">\n" +
               "      <h2 style=\"color: #d4af37; margin-top: 0;\">Reset Your Password</h2>\n" +
               "      <p style=\"font-size: 16px; line-height: 1.5; margin-bottom: 25px;\">Hi " + name + ",<br><br>We received a request to reset your password. Click the button below to choose a new password.</p>\n" +
               "      <a href=\"" + link + "\" style=\"background-color: #dc2626; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 4px; font-weight: bold; font-size: 16px; display: inline-block;\">Reset Password</a>\n" +
               "      <p style=\"font-size: 14px; color: #888; margin-top: 30px;\">This link will expire in 15 minutes.<br>If you did not request a password reset, you can safely ignore this email.</p>\n" +
               "    </div>\n" +
               "  </div>\n" +
               "</div>";
    }

    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        String hash = hash(rawToken);
        PasswordResetToken token = tokenRepository.findByTokenHash(hash).orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (token.getUsed() != null && token.getUsed()) throw new IllegalArgumentException("Token already used");
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) throw new IllegalArgumentException("Token expired");
        user u = userRepository.findById(token.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        u.setPassword(passwordEncoder.encode(newPassword));
        u.setTokenVersion(u.getTokenVersion() + 1);
        userRepository.save(u);
        
        token.setUsed(true);
        tokenRepository.save(token);
        
        refreshTokenService.revokeAllForUser(u.getId());
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
