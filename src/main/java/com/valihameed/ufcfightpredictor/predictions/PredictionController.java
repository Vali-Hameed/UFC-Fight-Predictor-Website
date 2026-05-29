package com.valihameed.ufcfightpredictor.predictions;

import com.valihameed.ufcfightpredictor.users.user;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/predictions")
@AllArgsConstructor
public class PredictionController {
    private final PredictionService predictionService;

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody PredictionRequest req, Authentication authentication) {
        user u = (user) authentication.getPrincipal();
        predictionService.submitPrediction(u.getId(), req);
        return ResponseEntity.ok().build();
    }
}
