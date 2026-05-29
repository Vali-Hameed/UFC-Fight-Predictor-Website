package com.valihameed.ufcfightpredictor.ml;

import com.valihameed.ufcfightpredictor.models.MlPrediction;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ml")
@AllArgsConstructor
public class MlController {
    private final MlService mlService;
    private final FightRepository fightRepository;

    @GetMapping("/fight/{fightId}")
    public ResponseEntity<?> predictForFight(@PathVariable Long fightId) {
        Fight fight = fightRepository.findById(fightId).orElse(null);
        if (fight == null) return ResponseEntity.notFound().build();
        String f1 = fight.getFighter1Name();
        String f2 = fight.getFighter2Name();
        if (f1 == null || f2 == null) return ResponseEntity.badRequest().body(Map.of("error","Fighter names missing"));
        try {
            MlPrediction p = mlService.getPrediction(f1, f2, fightId);
            CacheControl cc = CacheControl.maxAge(java.time.Duration.ofMinutes(mlService.getCacheTtlMinutes())).cachePublic();
            return ResponseEntity.ok().cacheControl(cc).body(Map.of("predicted_winner", p.getPredictedWinner(), "confidence_score", p.getConfidenceScore(), "cached_at", p.getCachedAt()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(503).body(Map.of("message", "Our prediction model is currently unavailable. Please try again later."));
        }
    }

    @PostMapping("/fight/{fightId}/refresh")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> refreshPrediction(@PathVariable Long fightId) {
        Fight fight = fightRepository.findById(fightId).orElse(null);
        if (fight == null) return ResponseEntity.notFound().build();
        try {
            MlPrediction p = mlService.forceRefreshPrediction(fight.getFighter1Name(), fight.getFighter2Name(), fightId);
            return ResponseEntity.ok(Map.of("predicted_winner", p.getPredictedWinner(), "confidence_score", p.getConfidenceScore(), "cached_at", p.getCachedAt()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(503).body(Map.of("message", "Failed to refresh ML prediction"));
        }
    }
}
