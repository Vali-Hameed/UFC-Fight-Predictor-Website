package com.valihameed.ufcfightpredictor.predictions;

import com.valihameed.ufcfightpredictor.models.UserPrediction;
import com.valihameed.ufcfightpredictor.repository.UserPredictionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@AllArgsConstructor
public class PredictionService {
    private final UserPredictionRepository userPredictionRepository;

    public UserPrediction submitPrediction(Long userId, PredictionRequest req) {
        // Prevent duplicate
        userPredictionRepository.findByUserIdAndFightId(userId, req.getFightId()).ifPresent(existing -> {
            throw new RuntimeException("Prediction already submitted for this fight");
        });
        UserPrediction p = UserPrediction.builder()
                .userId(userId)
                .fightId(req.getFightId())
                .predictedWinner(req.getPredictedWinner())
                .predictedMethod(req.getPredictedMethod())
                .predictedRound(req.getPredictedRound())
                .submittedAt(OffsetDateTime.now())
                .locked(false)
                .build();
        return userPredictionRepository.save(p);
    }
}
