package com.valihameed.ufcfightpredictor.users;

import com.valihameed.ufcfightpredictor.repository.userRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class userService implements UserDetailsService {
    private  final userRepository userRepository;
    private  final PasswordEncoder passwordEncoder;
    private  final BCryptPasswordEncoder bCryptPasswordEncoder;

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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
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
        // TODO: Send confirmation token
        return "it works";
    }
}
