package com.valihameed.ufcfightpredictor.registration;

import com.valihameed.ufcfightpredictor.repository.roleRepository;
import com.valihameed.ufcfightpredictor.users.role;
import com.valihameed.ufcfightpredictor.users.user;
import com.valihameed.ufcfightpredictor.users.userService;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class RegistrationService {
    private final userService userService;
    private final emailValidator emailValidator;
    private final roleRepository roleRepository;

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
}
