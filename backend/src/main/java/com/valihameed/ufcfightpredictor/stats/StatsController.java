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
    private final com.valihameed.ufcfightpredictor.repository.PredictionResultRepository predictionResultRepository;
    private final com.valihameed.ufcfightpredictor.repository.UserPredictionRepository userPredictionRepository;

    @GetMapping("/fix-db")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<String> fixDb() {
        java.util.List<com.valihameed.ufcfightpredictor.models.PredictionResult> allPrs = predictionResultRepository.findAll();
        int deleted = 0;
        for (var pr : allPrs) {
            var upOpt = userPredictionRepository.findById(pr.getUserPredictionId());
            if (upOpt.isPresent()) {
                var fOpt = fightRepository.findById(upOpt.get().getFightId());
                if (fOpt.isPresent()) {
                    var f = fOpt.get();
                    if (!"COMPLETED".equalsIgnoreCase(f.getStatus()) && !"CANCELED".equalsIgnoreCase(f.getStatus())) {
                        predictionResultRepository.delete(pr);
                        deleted++;
                    }
                }
            }
        }
        
        java.util.List<com.valihameed.ufcfightpredictor.models.Leaderboard> allLbs = leaderboardRepository.findAll();
        for (var lb : allLbs) {
            var predictions = userPredictionRepository.findByUserId(lb.getUserId());
            int totalPoints = 0;
            int totalPreds = 0;
            int correctPreds = 0;
            int currentStreak = 0;
            int bestStreak = 0;
            
            predictions.sort((a,b) -> a.getSubmittedAt().compareTo(b.getSubmittedAt()));
            
            for (var up : predictions) {
                var prList = predictionResultRepository.findByUserPredictionId(up.getId());
                if (!prList.isEmpty()) {
                    // Check if this prediction result is one of the ones we just deleted
                    // In JPA, it might still be returned in the same transaction if we do a fresh query, 
                    // but findByUserPredictionId might hit DB. We can filter deleted:
                    var pr = prList.get(0);
                    var fOpt = fightRepository.findById(up.getFightId());
                    if (fOpt.isPresent()) {
                        var f = fOpt.get();
                        if (!"COMPLETED".equalsIgnoreCase(f.getStatus()) && !"CANCELED".equalsIgnoreCase(f.getStatus())) {
                            continue; // Skip the ones we are deleting
                        }
                    }
                    totalPreds++;
                    totalPoints += pr.getPointsAwarded() != null ? pr.getPointsAwarded() : 0;
                    if (pr.getPointsAwarded() != null && pr.getPointsAwarded() > 0) {
                        correctPreds++;
                        currentStreak++;
                        if (currentStreak > bestStreak) bestStreak = currentStreak;
                    } else {
                        currentStreak = 0;
                    }
                }
            }
            lb.setTotalPoints(totalPoints);
            lb.setTotalPredictions(totalPreds);
            lb.setCorrectPredictions(correctPreds);
            lb.setCurrentStreak(currentStreak);
            lb.setBestStreak(bestStreak);
            leaderboardRepository.save(lb);
        }
        
        return ResponseEntity.ok("Deleted " + deleted + " bogus results and recomputed all leaderboards.");
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
