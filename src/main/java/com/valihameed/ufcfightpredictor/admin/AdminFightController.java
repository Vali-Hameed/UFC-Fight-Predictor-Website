package com.valihameed.ufcfightpredictor.admin;

import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.results.ResultProcessingService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/fights")
@AllArgsConstructor
public class AdminFightController {
    private final FightRepository fightRepository;
    private final ResultProcessingService resultProcessingService;

    @PostMapping("/{id}/result")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<?> enterResult(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Fight fight = fightRepository.findById(id).orElse(null);
        if (fight == null) return ResponseEntity.notFound().build();
        // expect keys: result_winner, result_method, result_round, result_time
        String winner = body.getOrDefault("result_winner", null) == null ? null : body.get("result_winner").toString();
        String method = body.getOrDefault("result_method", null) == null ? null : body.get("result_method").toString();
        Integer round = body.get("result_round") == null ? null : Integer.valueOf(body.get("result_round").toString());
        String time = body.getOrDefault("result_time", null) == null ? null : body.get("result_time").toString();
        fight.setResultWinner(winner);
        fight.setResultMethod(method);
        fight.setResultRound(round);
        fight.setResultTime(time);
        fight.setStatus("COMPLETED");
        fightRepository.save(fight);
        // process predictions
        resultProcessingService.processFightResult(id);
        return ResponseEntity.ok(Map.of("status", "processed"));
    }
}
