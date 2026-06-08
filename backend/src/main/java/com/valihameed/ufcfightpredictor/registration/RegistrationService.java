package com.valihameed.ufcfightpredictor.registration;

import com.valihameed.ufcfightpredictor.email.EmailSender;
import com.valihameed.ufcfightpredictor.registration.token.ConfirmationToken;
import com.valihameed.ufcfightpredictor.registration.token.ConfirmationTokenService;
import com.valihameed.ufcfightpredictor.repository.roleRepository;
import com.valihameed.ufcfightpredictor.users.role;
import com.valihameed.ufcfightpredictor.users.user;
import com.valihameed.ufcfightpredictor.users.userService;

import lombok.RequiredArgsConstructor;
import com.valihameed.ufcfightpredictor.util.InputSanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final userService userService;
    private final emailValidator emailValidator;
    private final roleRepository roleRepository;
    private final ConfirmationTokenService confirmationTokenService;
    private final EmailSender emailSender;
    private final InputSanitizer inputSanitizer;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public String register(RegistrationRequest request) {
        boolean isValidEmail = emailValidator.test(request.getEmail());
        if (!isValidEmail) {
            throw new IllegalArgumentException("Invalid email address");
        }
        // sanitize inputs
        String firstName = inputSanitizer.sanitize(request.getFirstName());
        String lastName = inputSanitizer.sanitize(request.getLastName());
        String email = inputSanitizer.sanitize(request.getEmail());
        String userName = inputSanitizer.sanitize(request.getUserName());
        role userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(new role(null, "ROLE_USER")));
        String token = userService.signUpUser(new user(firstName, lastName, userName, email, request.getPassword(), userRole));
        String link = frontendUrl + "/verify-email?token=" + token;
        emailSender.sendEmail(request.getEmail(), buildEmail(request.getFirstName(), link), "Confirm your email");
        return token;
    }
    @Transactional
    public String confirmToken(String token) {
        ConfirmationToken confirmationToken = confirmationTokenService
                .getToken(token)
                .orElseThrow(() ->
                        new IllegalStateException("token not found"));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new IllegalStateException("email already confirmed");
        }

        LocalDateTime expiredAt = confirmationToken.getExpiresAt();

        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("token expired");
        }

        confirmationTokenService.setConfirmedAt(token);
        userService.enableUser(
                confirmationToken.getUser().getEmail());
        return "confirmed";
    }

    @Transactional
    public String resendVerificationToken(String email) {
        user u = (user) userService.loadUserByEmail(email);
        if (u.isEnabled()) {
            throw new IllegalStateException("Account is already verified");
        }
        String token = userService.generateNewVerificationToken(u);
        String link = frontendUrl + "/verify-email?token=" + token;
        emailSender.sendEmail(u.getEmail(), buildEmail(u.getFirstName(), link), "Confirm your email");
        return "Verification email sent";
    }
    private String buildEmail(String name, String link) {
        return "<div style=\"font-family: Arial, sans-serif; background-color: #09090b; color: #ffffff; padding: 40px 20px; text-align: center;\">\n" +
               "  <div style=\"max-width: 600px; margin: 0 auto; background-color: #121212; border: 1px solid #333; border-radius: 8px; overflow: hidden;\">\n" +
               "    <div style=\"background-color: #dc2626; padding: 20px;\">\n" +
               "      <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">FightPicks</h1>\n" +
               "    </div>\n" +
               "    <div style=\"padding: 30px 20px;\">\n" +
               "      <h2 style=\"color: #d4af37; margin-top: 0;\">Confirm Your Email</h2>\n" +
               "      <p style=\"font-size: 16px; line-height: 1.5; margin-bottom: 25px;\">Hi " + name + ",<br><br>Welcome to FightPicks! Please confirm your email address to activate your account and start making predictions.</p>\n" +
               "      <a href=\"" + link + "\" style=\"background-color: #dc2626; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 4px; font-weight: bold; font-size: 16px; display: inline-block;\">Activate Account</a>\n" +
               "      <p style=\"font-size: 14px; color: #888; margin-top: 30px;\">This link will expire in 15 minutes.<br>If you did not create an account, please ignore this email.</p>\n" +
               "    </div>\n" +
               "  </div>\n" +
               "</div>";
    }

}
