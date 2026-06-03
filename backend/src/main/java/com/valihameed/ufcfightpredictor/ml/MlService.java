package com.valihameed.ufcfightpredictor.ml;

import com.valihameed.ufcfightpredictor.models.MlPrediction;
import com.valihameed.ufcfightpredictor.repository.MlPredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MlService {
    private final RestTemplate restTemplate;
    private final MlPredictionRepository mlPredictionRepository;

    @Value("${fastapi.base-url}")
    private String fastapiBase;

    @Value("${ml.cache-ttl-minutes:60}")
    private long cacheTtlMinutes;

    public long getCacheTtlMinutes() {
        return cacheTtlMinutes;
    }

    public MlPrediction getPrediction(String fighter1, String fighter2, Long fightId) {
        // validate existing cache
        var existing = mlPredictionRepository.findByFightId(fightId);
        if (existing.isPresent()) {
            MlPrediction p = existing.get();
            if (p.getCachedAt() != null && p.getCachedAt().isAfter(OffsetDateTime.now().minus(Duration.ofMinutes(cacheTtlMinutes)))) {
                if (p.getConfidenceScore() != null && !Double.isNaN(p.getConfidenceScore())) {
                    return p;
                }
            }
        }

        // call remote ML service
        try {
            String url = fastapiBase + "/predict?red_fighter_name={f1}&blue_fighter_name={f2}";
            Map resp = restTemplate.postForObject(url, null, Map.class, fighter1, fighter2);
            if (resp == null || !resp.containsKey("predicted_winner")) {
                throw new IllegalStateException("ML service response missing predicted_winner");
            }
            String winner = (String) resp.get("predicted_winner");
            Double confidence;
            if (winner != null && winner.equalsIgnoreCase(fighter1)) {
                confidence = Double.valueOf(resp.get("red_fighter_win_probability").toString());
            } else {
                confidence = Double.valueOf(resp.get("blue_fighter_win_probability").toString());
            }
            if (confidence.isNaN()) {
                throw new IllegalStateException("ML service returned NaN for confidence");
            }
            MlPrediction p = MlPrediction.builder().fightId(fightId).fighter1Name(fighter1).fighter2Name(fighter2).predictedWinner(winner).confidenceScore(confidence).cachedAt(OffsetDateTime.now()).build();
            // upsert
            existing.ifPresent(old -> p.setId(old.getId()));
            return mlPredictionRepository.save(p);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to retrieve ML prediction", e);
        }
    }

    public MlPrediction forceRefreshPrediction(String fighter1, String fighter2, Long fightId) {
        try {
            String url = fastapiBase + "/predict?red_fighter_name={f1}&blue_fighter_name={f2}";
            Map resp = restTemplate.postForObject(url, null, Map.class, fighter1, fighter2);
            if (resp == null || !resp.containsKey("predicted_winner")) {
                throw new IllegalStateException("ML service response missing predicted_winner");
            }
            String winner = (String) resp.get("predicted_winner");
            Double confidence;
            if (winner != null && winner.equalsIgnoreCase(fighter1)) {
                confidence = Double.valueOf(resp.get("red_fighter_win_probability").toString());
            } else {
                confidence = Double.valueOf(resp.get("blue_fighter_win_probability").toString());
            }
            if (confidence.isNaN()) {
                throw new IllegalStateException("ML service returned NaN for confidence");
            }
            MlPrediction p = MlPrediction.builder().fightId(fightId).fighter1Name(fighter1).fighter2Name(fighter2).predictedWinner(winner).confidenceScore(confidence).cachedAt(OffsetDateTime.now()).build();
            var existing = mlPredictionRepository.findByFightId(fightId);
            existing.ifPresent(old -> p.setId(old.getId()));
            return mlPredictionRepository.save(p);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh ML prediction", e);
        }
    }

    public MlPrediction getHypotheticalPrediction(String fighter1, String fighter2) {
        var existing = mlPredictionRepository.findFirstByFighter1NameAndFighter2NameOrderByCachedAtDesc(fighter1, fighter2);
        if (existing.isPresent()) {
            MlPrediction p = existing.get();
            if (p.getCachedAt() != null && p.getCachedAt().isAfter(OffsetDateTime.now().minus(Duration.ofMinutes(cacheTtlMinutes)))) {
                if (p.getConfidenceScore() != null && !Double.isNaN(p.getConfidenceScore())) {
                    return p;
                }
            }
        }
        
        try {
            String url = fastapiBase + "/predict?red_fighter_name={f1}&blue_fighter_name={f2}";
            Map resp = restTemplate.postForObject(url, null, Map.class, fighter1, fighter2);
            if (resp == null || !resp.containsKey("predicted_winner")) {
                throw new IllegalStateException("ML service response missing predicted_winner");
            }
            String winner = (String) resp.get("predicted_winner");
            Double confidence;
            if (winner != null && winner.equalsIgnoreCase(fighter1)) {
                confidence = Double.valueOf(resp.get("red_fighter_win_probability").toString());
            } else {
                confidence = Double.valueOf(resp.get("blue_fighter_win_probability").toString());
            }
            if (confidence.isNaN()) {
                throw new IllegalStateException("ML service returned NaN for confidence");
            }
            MlPrediction p = MlPrediction.builder()
                .fighter1Name(fighter1)
                .fighter2Name(fighter2)
                .predictedWinner(winner)
                .confidenceScore(confidence)
                .cachedAt(OffsetDateTime.now())
                .build();
                
            if (existing.isPresent()) {
                MlPrediction old = existing.get();
                if (old.getFightId() == null) {
                    p.setId(old.getId());
                }
            }
            return mlPredictionRepository.save(p);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to retrieve ML prediction", e);
        }
    }
}
