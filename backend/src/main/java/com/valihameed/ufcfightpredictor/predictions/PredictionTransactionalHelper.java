package com.valihameed.ufcfightpredictor.predictions;

import com.valihameed.ufcfightpredictor.models.CommunityVote;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.models.UserPrediction;
import com.valihameed.ufcfightpredictor.repository.CommunityVoteRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.UserPredictionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Handles the transactional database work for predictions.
 * Separated from PredictionService so that @Transactional is invoked
 * through Spring's proxy (not via self-invocation).
 */
@Slf4j
@Component
@AllArgsConstructor
public class PredictionTransactionalHelper {
    private final UserPredictionRepository userPredictionRepository;
    private final FightRepository fightRepository;
    private final CommunityVoteRepository communityVoteRepository;

    @Transactional
    public UserPrediction doSubmitPrediction(Long userId, PredictionRequest req) {
        Fight fight = fightRepository.findById(req.getFightId())
                .orElseThrow(() -> new IllegalArgumentException("Fight not found"));

        if ("COMPLETED".equals(fight.getStatus()) || "LIVE".equals(fight.getStatus())) {
            throw new IllegalStateException("Fight is locked for predictions");
        }

        UserPrediction existing = userPredictionRepository
                .findFirstByUserIdAndFightId(userId, req.getFightId())
                .orElse(null);

        if (existing != null && Boolean.TRUE.equals(existing.getLocked())) {
            throw new IllegalStateException("Prediction is locked");
        }

        String oldPredictedWinner = existing != null ? existing.getPredictedWinner() : null;

        UserPrediction p = existing != null ? existing : UserPrediction.builder()
                .userId(userId)
                .fightId(req.getFightId())
                .build();

        p.setPredictedWinner(req.getPredictedWinner());
        p.setPredictedMethod(req.getPredictedMethod());
        p.setPredictedRound(req.getPredictedRound());
        p.setSubmittedAt(OffsetDateTime.now());
        p.setLocked(false);

        // Update CommunityVote only when the pick actually changed
        if (oldPredictedWinner == null || !oldPredictedWinner.equals(req.getPredictedWinner())) {
            updateCommunityVote(req.getFightId(), fight, oldPredictedWinner, req.getPredictedWinner());
        }

        return userPredictionRepository.save(p);
    }

    private void updateCommunityVote(Long fightId, Fight fight, String oldWinner, String newWinner) {
        CommunityVote vote = communityVoteRepository.findByFightId(fightId).orElse(null);

        if (vote == null) {
            vote = CommunityVote.builder()
                    .fightId(fightId)
                    .fighter1Votes(0)
                    .fighter2Votes(0)
                    .build();
            try {
                communityVoteRepository.saveAndFlush(vote);
            } catch (DataIntegrityViolationException e) {
                log.debug("CommunityVote concurrent insert for fightId={}, re-fetching", fightId);
                vote = communityVoteRepository.findByFightId(fightId)
                        .orElseThrow(() -> new IllegalStateException("CommunityVote disappeared after concurrent insert"));
            }
        }

        // Subtract old vote if any
        if (oldWinner != null) {
            if (oldWinner.equals(fight.getFighter1Name())) {
                vote.setFighter1Votes(Math.max(0, safeVotes(vote.getFighter1Votes()) - 1));
            } else if (oldWinner.equals(fight.getFighter2Name())) {
                vote.setFighter2Votes(Math.max(0, safeVotes(vote.getFighter2Votes()) - 1));
            }
        }

        // Add new vote
        if (newWinner.equals(fight.getFighter1Name())) {
            vote.setFighter1Votes(safeVotes(vote.getFighter1Votes()) + 1);
        } else if (newWinner.equals(fight.getFighter2Name())) {
            vote.setFighter2Votes(safeVotes(vote.getFighter2Votes()) + 1);
        }

        vote.setLastUpdated(OffsetDateTime.now());
        communityVoteRepository.save(vote);
    }

    private int safeVotes(Integer votes) {
        return votes == null ? 0 : votes;
    }
}
