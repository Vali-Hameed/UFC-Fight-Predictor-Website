package com.valihameed.ufcfightpredictor.registration;

import com.valihameed.ufcfightpredictor.registration.token.ConfirmationToken;
import com.valihameed.ufcfightpredictor.registration.token.ConfirmationTokenService;
import com.valihameed.ufcfightpredictor.repository.roleRepository;
import com.valihameed.ufcfightpredictor.users.role;
import com.valihameed.ufcfightpredictor.users.user;
import com.valihameed.ufcfightpredictor.users.userService;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class RegistrationService {
    private final userService userService;
    private final emailValidator emailValidator;
    private final roleRepository roleRepository;
    private final ConfirmationTokenService confirmationTokenService;

    @Transactional
    public String register(RegistrationRequest request) {
        boolean isValidEmail = emailValidator.test(request.getEmail());
        if (!isValidEmail) {
            throw new IllegalArgumentException("Invalid email address");
        }
        role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new role(null, "ROLE_USER")));
        return userService.signUpUser(new user(request.getFirstName(),request.getLastName(),request.getUserName(),request.getEmail(),request.getPassword(),userRole));
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

}
