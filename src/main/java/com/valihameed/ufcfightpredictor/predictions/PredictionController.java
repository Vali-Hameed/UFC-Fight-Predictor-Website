package com.valihameed.ufcfightpredictor.predictions;

import com.valihameed.ufcfightpredictor.users.user;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/predictions")
@AllArgsConstructor
public class PredictionController {
    private final PredictionService predictionService;
    private final com.valihameed.ufcfightpredictor.util.InputSanitizer inputSanitizer;

    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody PredictionRequest req, Authentication authentication) {
        user u = (user) authentication.getPrincipal();
        // sanitize string inputs
        req.setPredictedMethod(inputSanitizer.sanitize(req.getPredictedMethod()));
        req.setPredictedWinner(inputSanitizer.sanitize(req.getPredictedWinner()));
        predictionService.submitPrediction(u.getId(), req);
        return ResponseEntity.ok().build();
    }
}
