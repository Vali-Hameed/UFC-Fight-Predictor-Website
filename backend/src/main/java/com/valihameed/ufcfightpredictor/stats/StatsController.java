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
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @GetMapping("/fix-db")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<String> fixDb() {
        // Delete bogus results instantly
        int deleted = jdbcTemplate.update(
            "DELETE FROM prediction_results WHERE id IN (" +
            "  SELECT pr.id FROM prediction_results pr " +
            "  JOIN user_predictions up ON pr.user_prediction_id = up.id " +
            "  JOIN fights f ON up.fight_id = f.id " +
            "  WHERE f.status NOT IN ('COMPLETED', 'CANCELED')" +
            ")"
        );
        
        // Recalculate basic leaderboard stats (Points, Total Predictions, Correct Predictions)
        jdbcTemplate.update(
            "UPDATE leaderboards lb SET " +
            "total_points = COALESCE((SELECT SUM(pr.points_awarded) FROM prediction_results pr JOIN user_predictions up ON pr.user_prediction_id = up.id WHERE up.user_id = lb.user_id), 0), " +
            "total_predictions = COALESCE((SELECT COUNT(pr.id) FROM prediction_results pr JOIN user_predictions up ON pr.user_prediction_id = up.id WHERE up.user_id = lb.user_id), 0), " +
            "correct_predictions = COALESCE((SELECT COUNT(pr.id) FROM prediction_results pr JOIN user_predictions up ON pr.user_prediction_id = up.id WHERE up.user_id = lb.user_id AND pr.points_awarded > 0), 0)"
        );

        return ResponseEntity.ok("Fixed database instantly! Deleted " + deleted + " bogus results and recomputed points/counts.");
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
