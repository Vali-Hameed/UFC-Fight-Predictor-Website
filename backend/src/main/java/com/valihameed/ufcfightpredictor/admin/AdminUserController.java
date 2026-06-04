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
        return userRepository.findAll().stream()
            .filter(u -> u.getUsername() == null || !u.getUsername().startsWith("deleted_user_"))
            .toList();
    }

    @PatchMapping("/{id}/ban")
    public ResponseEntity<?> ban(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean locked) {
        return userRepository.findById(id)
            .map(u -> {
                if (u.getRole() != null && "ROLE_ADMIN".equals(u.getRole().getName())) {
                    return ResponseEntity.status(403).body(Map.of("error", "Cannot modify admin accounts via API"));
                }
                u.setLocked(locked);
                return ResponseEntity.ok(userRepository.save(u));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<?> setRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        return userRepository.findById(id)
            .map(u -> {
                if (u.getRole() != null && "ROLE_ADMIN".equals(u.getRole().getName())) {
                    return ResponseEntity.status(403).body(Map.of("error", "Cannot demote or modify admin accounts via API"));
                }
                role r = roleRepository.findByName(request.getRole())
                    .orElseGet(() -> roleRepository.save(role.builder().name(request.getRole()).build()));
                u.setRole(r);
                return ResponseEntity.ok(userRepository.save(u));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(u -> {
                if (u.getRole() != null && "ROLE_ADMIN".equals(u.getRole().getName())) {
                    return ResponseEntity.status(403).body(Map.of("error", "Cannot delete admin accounts"));
                }
                u.setUsername("deleted_user_" + u.getId());
                u.setEmail("deleted_" + u.getId() + "@deleted.com");
                u.setPassword("");
                u.setFirstName("Deleted");
                u.setLastName("User");
                u.setLocked(true);
                u.setEnabled(false);
                u.setProfileImageUrl(null);
                userRepository.save(u);
                return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Data
    public static class RoleRequest {
        private String role;
    }
}