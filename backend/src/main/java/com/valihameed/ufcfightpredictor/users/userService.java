package com.valihameed.ufcfightpredictor.users;

import com.valihameed.ufcfightpredictor.registration.token.ConfirmationToken;
import com.valihameed.ufcfightpredictor.registration.token.ConfirmationTokenService;
import com.valihameed.ufcfightpredictor.repository.UsernameHistoryRepository;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class userService implements UserDetailsService {
    private  final userRepository userRepository;
    private  final PasswordEncoder passwordEncoder;
    private  final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UsernameHistoryRepository usernameHistoryRepository;
    private final static String USER_NOT_FOUND_MSG = "user with %s not found";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"
    );
    private final ConfirmationTokenService confirmationTokenService;

    public user createNewUser(String username, String email, String password, role role) {
        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new IllegalStateException("Username already taken");
        }
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
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
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username or email cannot be empty");
        }
        String cleanInput = usernameOrEmail.trim();
        // Try username first (case-insensitive), then email (case-insensitive)
        return userRepository.findByUsernameIgnoreCase(cleanInput)
                .or(() -> userRepository.findByEmailIgnoreCase(cleanInput))
                .orElseThrow(() -> new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG, cleanInput)));
    }

    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
    }
    public String signUpUser(user user){
        com.valihameed.ufcfightpredictor.users.user existingUser = userRepository.findByEmailIgnoreCase(user.getEmail()).orElse(null);
        if(existingUser != null){
            if (existingUser.isEnabled()) {
                throw new IllegalStateException("Email is already registered");
            } else {
                throw new IllegalStateException("Email is registered but not verified");
            }
        }
        com.valihameed.ufcfightpredictor.users.user existingUsername = userRepository.findByUsernameIgnoreCase(user.getUsername()).orElse(null);
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

    public void changeUsername(user currentUser, String newUsername) {
        if (currentUser.getUsername().equalsIgnoreCase(newUsername)) {
            throw new IllegalStateException("New username must be different from the current one.");
        }

        // 1. Check if the current user has changed their username in the last 90 days
        Optional<UsernameHistory> lastChange = usernameHistoryRepository.findFirstByUserIdOrderByChangedAtDesc(currentUser.getId());
        if (lastChange.isPresent()) {
            OffsetDateTime ninetyDaysAgo = OffsetDateTime.now().minusDays(90);
            if (lastChange.get().getChangedAt().isAfter(ninetyDaysAgo)) {
                throw new IllegalStateException("You can only change your username once every 90 days.");
            }
        }

        // 2. Check if the new username is currently in use in the 'user' table
        Optional<user> existingUser = userRepository.findByUsernameIgnoreCase(newUsername);
        if (existingUser.isPresent()) {
            throw new IllegalStateException("Username is already taken.");
        }

        // 3. Check if the new username was recently used and is under a 14-day hold
        OffsetDateTime fourteenDaysAgo = OffsetDateTime.now().minusDays(14);
        List<UsernameHistory> recentHistory = usernameHistoryRepository.findRecentByPreviousUsername(newUsername, fourteenDaysAgo);
        
        for (UsernameHistory history : recentHistory) {
            // Allow if the history belongs to the same user (reverting to their own recent username)
            if (!history.getUser().getId().equals(currentUser.getId())) {
                throw new IllegalStateException("This username is currently reserved. Try again later.");
            }
        }

        // Everything is fine. Save the current username to history
        UsernameHistory historyEntry = UsernameHistory.builder()
                .user(currentUser)
                .previousUsername(currentUser.getUsername())
                .changedAt(OffsetDateTime.now())
                .build();
        usernameHistoryRepository.save(historyEntry);

        // Update the user's username and invalidate old tokens
        currentUser.setUsername(newUsername);
        currentUser.setTokenVersion(currentUser.getTokenVersion() + 1);
        userRepository.save(currentUser);
    }
}
