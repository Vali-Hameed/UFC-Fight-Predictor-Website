package com.valihameed.ufcfightpredictor.password;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/password")
@AllArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<?> requestReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String raw = passwordResetService.createPasswordReset(email);
        return ResponseEntity.ok(Map.of("status","sent"));
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmReset(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String password = body.get("password");
        passwordResetService.confirmPasswordReset(token, password);
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}
