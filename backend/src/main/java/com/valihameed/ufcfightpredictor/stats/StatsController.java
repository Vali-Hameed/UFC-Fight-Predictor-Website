package com.valihameed.ufcfightpredictor.stats;

import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.LeaderboardRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.valihameed.ufcfightpredictor.models.Fight;

@RestController
@RequestMapping("/api/v1/stats")
@AllArgsConstructor
public class StatsController {

    private final FightRepository fightRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final com.valihameed.ufcfightpredictor.results.ResultProcessingService resultProcessingService;

    @GetMapping("/reset-event/{eventId}")
    public ResponseEntity<String> resetEvent(@PathVariable Long eventId) {
        resultProcessingService.rollbackEvent(eventId);
        return ResponseEntity.ok("Event " + eventId + " completely rolled back! The scraper will start re-evaluating its fights shortly.");
    }

    @GetMapping("/reset-fight/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<String> resetFight(@PathVariable Long id) {
        Fight fight = fightRepository.findById(id).orElseThrow();
        fight.setStatus("UPCOMING");
        fight.setResultWinner(null);
        fight.setResultMethod(null);
        fight.setLiveStatus(null);
        fightRepository.save(fight);
        return ResponseEntity.ok("Fight " + id + " reset to UPCOMING. The scraper will catch it on the next poll.");
    }

    @GetMapping("/global-accuracy")
    public ResponseEntity<GlobalAccuracyDto> getGlobalAccuracy() {
        java.time.OffsetDateTime modernEraStart = java.time.OffsetDateTime.parse("2020-01-01T00:00:00Z");
        Long correctAi = fightRepository.countCorrectAiPredictions(modernEraStart);
        Long totalAi = fightRepository.countTotalAiPredictions(modernEraStart);
        Long correctCommunity = leaderboardRepository.sumCorrectPredictions();
        Long totalCommunity = leaderboardRepository.sumTotalPredictions();

        int aiAccuracy = (totalAi != null && totalAi > 0) ? (int) Math.round(((double) correctAi / totalAi) * 100) : 0;
        int communityAccuracy = (totalCommunity != null && totalCommunity > 0) ? (int) Math.round(((double) correctCommunity / totalCommunity) * 100) : 0;

        GlobalAccuracyDto dto = new GlobalAccuracyDto(aiAccuracy, communityAccuracy, totalAi, totalCommunity);
        return ResponseEntity.ok(dto);
    }

    @Data
    @AllArgsConstructor
    public static class GlobalAccuracyDto {
        private int aiAccuracy;
        private int communityAccuracy;
        private Long totalAiFights;
        private Long totalCommunityPredictions;
    }
}
