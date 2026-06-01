package com.valihameed.ufcfightpredictor.predictions;

import com.valihameed.ufcfightpredictor.models.CommunityVote;
import com.valihameed.ufcfightpredictor.models.Fight;
import com.valihameed.ufcfightpredictor.models.UserPrediction;
import com.valihameed.ufcfightpredictor.repository.CommunityVoteRepository;
import com.valihameed.ufcfightpredictor.repository.FightRepository;
import com.valihameed.ufcfightpredictor.repository.UserPredictionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PredictionService {
    private final UserPredictionRepository userPredictionRepository;
    private final FightRepository fightRepository;
    private final CommunityVoteRepository communityVoteRepository;
    private final PredictionTransactionalHelper txHelper;

    /**
     * In-memory lock map: key = "userId:fightId".
     * If a key is present, a request for that user+fight is currently being processed.
     * This prevents concurrent DB access entirely — no connection pool exhaustion.
     */
    private final ConcurrentHashMap<String, Boolean> activeRequests = new ConcurrentHashMap<>();

    public PredictionService(UserPredictionRepository userPredictionRepository,
                             FightRepository fightRepository,
                             CommunityVoteRepository communityVoteRepository,
                             PredictionTransactionalHelper txHelper) {
        this.userPredictionRepository = userPredictionRepository;
        this.fightRepository = fightRepository;
        this.communityVoteRepository = communityVoteRepository;
        this.txHelper = txHelper;
    }

    /**
     * Submits or updates a prediction. Uses an in-memory lock per user+fight
     * so that only ONE request at a time reaches the database. All concurrent
     * duplicates are immediately rejected with a 409 — no DB connections consumed.
     */
    public UserPrediction submitPrediction(Long userId, PredictionRequest req) {
        String lockKey = userId + ":" + req.getFightId();

        // Try to acquire the in-memory lock. If another request for the same
        // user+fight is already processing, reject immediately.
        if (activeRequests.putIfAbsent(lockKey, Boolean.TRUE) != null) {
            throw new IllegalStateException("Prediction already being processed, please wait.");
        }

        try {
            return txHelper.doSubmitPrediction(userId, req);
        } finally {
            activeRequests.remove(lockKey);
        }
    }
}
