package com.valihameed.ufcfightpredictor.users;

import com.valihameed.ufcfightpredictor.registration.token.ConformationToken;
import com.valihameed.ufcfightpredictor.registration.token.ConformationTokenService;
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
    private final ConformationTokenService conformationTokenService;

    public user createNewUser(String username, String email, String password, role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        user user = com.valihameed.ufcfightpredictor.users.user.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role != null ? role : role) // default to USER
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
        boolean present= userRepository.findByEmail(user.getEmail()).isPresent();
        if(present){
            throw new RuntimeException("Email already taken");
        }
        String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);
        String token=UUID.randomUUID().toString();
        ConformationToken conformationToken = new ConformationToken(token, LocalDateTime.now(),LocalDateTime.now().plusMinutes(15),user);
        conformationTokenService.saveConformationToken(conformationToken);
        return token;
        // TODO: SEND EMAIL
    }
}
