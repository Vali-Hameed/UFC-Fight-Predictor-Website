package com.valihameed.ufcfightpredictor.results;

import com.valihameed.ufcfightpredictor.models.*;
import com.valihameed.ufcfightpredictor.repository.*;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ResultProcessingService {
    private static final Logger log = LoggerFactory.getLogger(ResultProcessingService.class);

    private final FightRepository fightRepository;
    private final UserPredictionRepository userPredictionRepository;
    private final PredictionResultRepository predictionResultRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void processFightResult(Long fightId) {
        Fight fight = fightRepository.findById(fightId).orElseThrow(() -> new IllegalArgumentException("Fight not found"));
        if (fight.getResultWinner() == null) {
            throw new IllegalArgumentException("Fight result not set");
        }
        List<UserPrediction> predictions = userPredictionRepository.findByFightId(fightId);
        for (UserPrediction up : predictions) {
            // avoid double-processing
            List<PredictionResult> existing = predictionResultRepository.findByUserPredictionId(up.getId());
            if (!existing.isEmpty()) continue;

            boolean winnerCorrect = fight.getResultWinner().equalsIgnoreCase(up.getPredictedWinner());
            boolean methodCorrect = fight.getResultMethod() != null && fight.getResultMethod().equalsIgnoreCase(up.getPredictedMethod());
            boolean roundCorrect = fight.getResultRound() != null && up.getPredictedRound() != null && fight.getResultRound().equals(up.getPredictedRound());

            int points = 0;
            if (winnerCorrect) points += 10;
            if (methodCorrect) points += 5;
            if (roundCorrect) points += 3;

            PredictionResult pr = PredictionResult.builder()
                    .userPredictionId(up.getId())
                    .isWinnerCorrect(winnerCorrect)
                    .isMethodCorrect(methodCorrect)
                    .isRoundCorrect(roundCorrect)
                    .pointsAwarded(points)
                    .build();
            predictionResultRepository.save(pr);

            // update leaderboard
            Long userId = up.getUserId();
            Leaderboard lb = leaderboardRepository.findByUserId(userId).orElseGet(() -> Leaderboard.builder().userId(userId).totalPoints(0).correctPredictions(0).totalPredictions(0).currentStreak(0).bestStreak(0).build());
            lb.setTotalPoints((lb.getTotalPoints() == null ? 0 : lb.getTotalPoints()) + points);
            lb.setTotalPredictions((lb.getTotalPredictions() == null ? 0 : lb.getTotalPredictions()) + 1);
            if (winnerCorrect) {
                lb.setCorrectPredictions((lb.getCorrectPredictions() == null ? 0 : lb.getCorrectPredictions()) + 1);
                lb.setCurrentStreak((lb.getCurrentStreak() == null ? 0 : lb.getCurrentStreak()) + 1);
                lb.setBestStreak(Math.max(lb.getBestStreak() == null ? 0 : lb.getBestStreak(), lb.getCurrentStreak()));
            } else {
                lb.setCurrentStreak(0);
            }
            lb.setLastUpdated(OffsetDateTime.now());
            leaderboardRepository.save(lb);

            // notification
            Notification note = Notification.builder()
                    .userId(userId)
                    .type("RESULT")
                    .message(String.format("Your prediction for fight %d scored %d points.", fightId, points))
                    .read(false)
                    .createdAt(OffsetDateTime.now())
                    .build();
            notificationRepository.save(note);
        }
    }
}
