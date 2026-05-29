package com.valihameed.ufcfightpredictor.admin;

import com.valihameed.ufcfightpredictor.repository.roleRepository;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.users.role;
import com.valihameed.ufcfightpredictor.users.user;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@AllArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {
    private final userRepository userRepository;
    private final roleRepository roleRepository;

    @GetMapping
    public List<user> listUsers() {
        return userRepository.findAll();
    }

    @PatchMapping("/{id}/ban")
    public ResponseEntity<user> ban(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean locked) {
        return userRepository.findById(id)
            .map(u -> {
                u.setLocked(locked);
                return ResponseEntity.ok(userRepository.save(u));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<user> setRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        return userRepository.findById(id)
            .map(u -> {
                role r = roleRepository.findByName(request.getRole())
                    .orElseGet(() -> roleRepository.save(role.builder().name(request.getRole()).build()));
                u.setRole(r);
                return ResponseEntity.ok(userRepository.save(u));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Data
    public static class RoleRequest {
        private String role;
    }
}