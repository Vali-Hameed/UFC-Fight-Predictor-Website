package com.valihameed.ufcfightpredictor.results;

import com.valihameed.ufcfightpredictor.models.*;
import com.valihameed.ufcfightpredictor.repository.*;
import com.valihameed.ufcfightpredictor.rewards.RewardService;
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
    private final com.valihameed.ufcfightpredictor.notifications.NotificationService notificationService;
    private final EventRepository eventRepository;
    private final EventLeaderboardRepository eventLeaderboardRepository;
    private final SeasonLeaderboardRepository seasonLeaderboardRepository;
    private final RewardService rewardService;

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
            if (!existing.isEmpty()) {
                PredictionResult old = existing.get(0);
                
                // Rollback leaderboard
                leaderboardRepository.findByUserId(up.getUserId()).ifPresent(lb -> {
                    lb.setTotalPredictions(Math.max(0, (lb.getTotalPredictions() != null ? lb.getTotalPredictions() : 0) - 1));
                    if (old.getPointsAwarded() > 0) {
                        lb.setTotalPoints(Math.max(0, (lb.getTotalPoints() != null ? lb.getTotalPoints() : 0) - old.getPointsAwarded()));
                        lb.setCorrectPredictions(Math.max(0, (lb.getCorrectPredictions() != null ? lb.getCorrectPredictions() : 0) - 1));
                    }
                    leaderboardRepository.save(lb);
                });
                
                predictionResultRepository.delete(old);
            }

            boolean predictedMethodProvided = up.getPredictedMethod() != null && !up.getPredictedMethod().trim().isEmpty() && !up.getPredictedMethod().equalsIgnoreCase("Any Method");
            boolean predictedRoundProvided = up.getPredictedRound() != null && up.getPredictedRound() > 0;

            boolean winnerCorrect = fight.getResultWinner().equalsIgnoreCase(up.getPredictedWinner());
            
            boolean methodCorrect = false;
            if (predictedMethodProvided) {
                methodCorrect = fight.getResultMethod() != null && fight.getResultMethod().equalsIgnoreCase(up.getPredictedMethod());
            }

            boolean roundCorrect = false;
            if (predictedRoundProvided) {
                roundCorrect = fight.getResultRound() != null && fight.getResultRound().equals(up.getPredictedRound());
            }

            boolean isUserPredictionDrawOrNC = "Canceled".equalsIgnoreCase(up.getPredictedWinner()) 
                    || "No Contest".equalsIgnoreCase(up.getPredictedWinner()) 
                    || "Canceled/No Contest".equalsIgnoreCase(up.getPredictedWinner())
                    || "Draw".equalsIgnoreCase(up.getPredictedWinner());

            int points = 0;
            if (winnerCorrect) {
                if (isUserPredictionDrawOrNC) {
                    points = 20;
                } else {
                    boolean methodFailed = predictedMethodProvided && !methodCorrect;
                    boolean roundFailed = predictedRoundProvided && !roundCorrect;

                    if (!methodFailed && !roundFailed) {
                        points += 10; // Base winner
                        if (predictedMethodProvided) points += 4; // Method
                        if (predictedRoundProvided) points += 7; // Round
                        if (predictedMethodProvided && predictedRoundProvided) points += 10; // Bonus
                    }
                }
            }

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
            
            boolean isFightCancelledOrNC = "Canceled".equalsIgnoreCase(fight.getResultWinner()) 
                    || "No Contest".equalsIgnoreCase(fight.getResultWinner()) 
                    || "Canceled/No Contest".equalsIgnoreCase(fight.getResultWinner());
            
            boolean userPredictedCancelledOrNC = "Canceled".equalsIgnoreCase(up.getPredictedWinner()) 
                    || "No Contest".equalsIgnoreCase(up.getPredictedWinner()) 
                    || "Canceled/No Contest".equalsIgnoreCase(up.getPredictedWinner());

            if (!winnerCorrect && isFightCancelledOrNC && !userPredictedCancelledOrNC) {
                // User predicted a normal fight, but it was cancelled.
                // Do not increment totalPredictions, correctPredictions, and do not reset streak.
            } else {
                lb.setTotalPoints((lb.getTotalPoints() == null ? 0 : lb.getTotalPoints()) + points);
                lb.setTotalPredictions((lb.getTotalPredictions() == null ? 0 : lb.getTotalPredictions()) + 1);
                
                if (points > 0) {
                    lb.setCorrectPredictions((lb.getCorrectPredictions() == null ? 0 : lb.getCorrectPredictions()) + 1);
                    lb.setCurrentStreak((lb.getCurrentStreak() == null ? 0 : lb.getCurrentStreak()) + 1);
                    lb.setBestStreak(Math.max(lb.getBestStreak() == null ? 0 : lb.getBestStreak(), lb.getCurrentStreak()));
                } else {
                    lb.setCurrentStreak(0);
                }

                // --- Update Event Leaderboard ---
                if (fight.getEventId() != null) {
                    EventLeaderboard elb = eventLeaderboardRepository
                            .findByEventIdAndUserId(fight.getEventId(), userId)
                            .orElseGet(() -> EventLeaderboard.builder()
                                    .eventId(fight.getEventId()).userId(userId)
                                    .totalPoints(0).correctPredictions(0).totalPredictions(0)
                                    .build());
                    elb.setTotalPoints(elb.getTotalPoints() + points);
                    elb.setTotalPredictions(elb.getTotalPredictions() + 1);
                    if (points > 0) {
                        elb.setCorrectPredictions(elb.getCorrectPredictions() + 1);
                    }
                    elb.setLastUpdated(OffsetDateTime.now());
                    eventLeaderboardRepository.save(elb);
                }

                // --- Update Season Leaderboard ---
                try {
                    Season season = rewardService.getOrCreateCurrentSeason();
                    SeasonLeaderboard slb = seasonLeaderboardRepository
                            .findBySeasonIdAndUserId(season.getId(), userId)
                            .orElseGet(() -> SeasonLeaderboard.builder()
                                    .seasonId(season.getId()).userId(userId)
                                    .totalPoints(0).correctPredictions(0).totalPredictions(0)
                                    .currentStreak(0).bestStreak(0)
                                    .build());
                    slb.setTotalPoints(slb.getTotalPoints() + points);
                    slb.setTotalPredictions(slb.getTotalPredictions() + 1);
                    if (points > 0) {
                        slb.setCorrectPredictions(slb.getCorrectPredictions() + 1);
                        slb.setCurrentStreak(slb.getCurrentStreak() + 1);
                        slb.setBestStreak(Math.max(slb.getBestStreak(), slb.getCurrentStreak()));
                    } else {
                        slb.setCurrentStreak(0);
                    }
                    slb.setLastUpdated(OffsetDateTime.now());
                    seasonLeaderboardRepository.save(slb);
                } catch (Exception e) {
                    log.warn("Failed to update season leaderboard for user {}: {}", userId, e.getMessage());
                }

                // --- Check streak badges ---
                rewardService.checkStreakBadges(userId, lb.getCurrentStreak());
            }
            
            lb.setLastUpdated(OffsetDateTime.now());
            leaderboardRepository.save(lb);

            // notification
            if (up.getOptOutResultNotification() == null || !up.getOptOutResultNotification()) {
                String eventName = "Unknown Event";
                if (fight.getEventId() != null) {
                    eventName = eventRepository.findById(fight.getEventId())
                            .map(Event::getName)
                            .orElse("Unknown Event");
                }
                
                String fightName = fight.getFighter1Name() + " vs " + fight.getFighter2Name();
                String message = String.format("Your prediction for %s : %s scored %d points.", eventName, fightName, points);

                Notification note = Notification.builder()
                        .userId(userId)
                        .type("RESULT")
                        .message(message)
                        .build();
                notificationService.createNotification(note);
            }
        }

        // --- Check if all fights in the event are completed, then distribute event rewards ---
        if (fight.getEventId() != null) {
            List<Fight> eventFights = fightRepository.findByEventIdOrderByFightOrderAsc(fight.getEventId());
            boolean allCompleted = eventFights.stream()
                    .allMatch(f -> "COMPLETED".equalsIgnoreCase(f.getStatus()) || "CANCELED".equalsIgnoreCase(f.getStatus()));
            if (allCompleted && !eventFights.isEmpty()) {
                try {
                    rewardService.distributeEventRewards(fight.getEventId());
                    log.info("Auto-distributed event rewards for event {}", fight.getEventId());
                } catch (Exception e) {
                    log.warn("Failed to auto-distribute event rewards for event {}: {}", fight.getEventId(), e.getMessage());
                }
            }
        }
    }

    @Transactional
    public void rollbackEvent(Long eventId) {
        List<Fight> fights = fightRepository.findByEventIdOrderByFightOrderAsc(eventId);
        for (Fight fight : fights) {
            List<UserPrediction> ups = userPredictionRepository.findByFightId(fight.getId());
            for (UserPrediction up : ups) {
                List<PredictionResult> prs = predictionResultRepository.findByUserPredictionId(up.getId());
                for (PredictionResult pr : prs) {
                    leaderboardRepository.findByUserId(up.getUserId()).ifPresent(lb -> {
                        lb.setTotalPoints(Math.max(0, (lb.getTotalPoints() != null ? lb.getTotalPoints() : 0) - pr.getPointsAwarded()));
                        lb.setTotalPredictions(Math.max(0, (lb.getTotalPredictions() != null ? lb.getTotalPredictions() : 0) - 1));
                        if (pr.getPointsAwarded() > 0) {
                            lb.setCorrectPredictions(Math.max(0, (lb.getCorrectPredictions() != null ? lb.getCorrectPredictions() : 0) - 1));
                        }
                        leaderboardRepository.save(lb);
                    });
                    predictionResultRepository.delete(pr);
                }
            }
            fight.setStatus("UPCOMING");
            fight.setResultWinner(null);
            fight.setResultMethod(null);
            fight.setLiveStatus(null);
            fightRepository.save(fight);
        }
    }
}
