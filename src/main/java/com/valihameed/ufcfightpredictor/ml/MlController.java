package com.valihameed.ufcfightpredictor.ml;

import com.valihameed.ufcfightpredictor.models.MlPrediction;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        MlPrediction p = mlService.getPrediction(f1, f2, fightId);
        if (p == null) {
            return ResponseEntity.ok(Map.of("message", "Our prediction model is currently unavailable. Please try again later."));
        }
        return ResponseEntity.ok(Map.of("predicted_winner", p.getPredictedWinner(), "confidence_score", p.getConfidenceScore(), "cached_at", p.getCachedAt()));
    }
}
