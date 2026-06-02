package com.valihameed.ufcfightpredictor.users;

import com.valihameed.ufcfightpredictor.registration.token.ConfirmationToken;
import com.valihameed.ufcfightpredictor.registration.token.ConfirmationTokenService;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class userService implements UserDetailsService {
    private  final userRepository userRepository;
    private  final PasswordEncoder passwordEncoder;
    private  final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final static String USER_NOT_FOUND_MSG = "user with %s not found";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"
    );
    private final ConfirmationTokenService confirmationTokenService;

    public user createNewUser(String username, String email, String password, role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Username already taken");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email already registered");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        user user = com.valihameed.ufcfightpredictor.users.user.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();

        return userRepository.save(user);

    }



    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // FIX: Use regex to reliably check if the input is an email.
        if (EMAIL_PATTERN.matcher(usernameOrEmail).matches()) {
            return userRepository.findByEmail(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG, usernameOrEmail)));
        } else {
            return userRepository.findByUsername(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG, usernameOrEmail)));
        }
    }

    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
    }
    public String signUpUser(user user){
        com.valihameed.ufcfightpredictor.users.user existingUser = userRepository.findByEmail(user.getEmail()).orElse(null);
        if(existingUser != null){
            if (existingUser.isEnabled()) {
                throw new IllegalStateException("Email is already registered");
            } else {
                throw new IllegalStateException("Email is registered but not verified");
            }
        }
        com.valihameed.ufcfightpredictor.users.user existingUsername = userRepository.findByUsername(user.getUsername()).orElse(null);
        if(existingUsername != null){
            if (existingUsername.isEnabled()) {
                throw new IllegalStateException("Username is already taken");
            } else {
                throw new IllegalStateException("Username is registered but not verified");
            }
        }
        String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);
        String token=UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(),LocalDateTime.now().plusMinutes(15),user);
        confirmationTokenService.saveConformationToken(confirmationToken);
        return token;

    }

    public String generateNewVerificationToken(user user) {
        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(), LocalDateTime.now().plusMinutes(15), user);
        confirmationTokenService.saveConformationToken(confirmationToken);
        return token;
    }

    public int enableUser(String email) {
        return userRepository.enableAppUser(email);
    }
}
