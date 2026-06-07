package com.valihameed.ufcfightpredictor.stats;

import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.LeaderboardRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@AllArgsConstructor
public class StatsController {

    private final FightRepository fightRepository;
    private final LeaderboardRepository leaderboardRepository;

    @GetMapping("/global-accuracy")
    public ResponseEntity<GlobalAccuracyDto> getGlobalAccuracy() {
        Long correctAi = fightRepository.countCorrectAiPredictions();
        Long totalAi = fightRepository.countTotalAiPredictions();

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
