package com.valihameed.ufcfightpredictor.users;

import com.valihameed.ufcfightpredictor.repository.userRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class userService {
    private  final userRepository userRepository;
    private  final PasswordEncoder passwordEncoder;

    public userService(userRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public user createNewUser(String username, String email, String password, role role) {
        user newUser = new user();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role);
        return userRepository.save(newUser);

    }
}
