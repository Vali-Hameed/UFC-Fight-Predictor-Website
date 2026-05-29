package com.valihameed.ufcfightpredictor.ml;

import com.valihameed.ufcfightpredictor.models.MlPrediction;
import com.valihameed.ufcfightpredictor.repository.MlPredictionRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@AllArgsConstructor
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
                return p;
            }
        }

        // call remote ML service
        try {
            String f1 = URLEncoder.encode(fighter1, StandardCharsets.UTF_8);
            String f2 = URLEncoder.encode(fighter2, StandardCharsets.UTF_8);
            String url = String.format("%s/predict?fighter1=%s&fighter2=%s", fastapiBase, f1, f2);
            Map resp = restTemplate.getForObject(url, Map.class);
            if (resp == null || !resp.containsKey("predicted_winner")) {
                throw new IllegalStateException("ML service response missing predicted_winner");
            }
            String winner = (String) resp.get("predicted_winner");
            Double confidence = Double.valueOf(resp.get("confidence_score").toString());
            MlPrediction p = MlPrediction.builder().fightId(fightId).predictedWinner(winner).confidenceScore(confidence).cachedAt(OffsetDateTime.now()).build();
            // upsert
            existing.ifPresent(old -> p.setId(old.getId()));
            return mlPredictionRepository.save(p);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to retrieve ML prediction", e);
        }
    }

    public MlPrediction forceRefreshPrediction(String fighter1, String fighter2, Long fightId) {
        try {
            String f1 = URLEncoder.encode(fighter1, StandardCharsets.UTF_8);
            String f2 = URLEncoder.encode(fighter2, StandardCharsets.UTF_8);
            String url = String.format("%s/predict?fighter1=%s&fighter2=%s", fastapiBase, f1, f2);
            Map resp = restTemplate.getForObject(url, Map.class);
            if (resp == null || !resp.containsKey("predicted_winner")) {
                throw new IllegalStateException("ML service response missing predicted_winner");
            }
            String winner = (String) resp.get("predicted_winner");
            Double confidence = Double.valueOf(resp.get("confidence_score").toString());
            MlPrediction p = MlPrediction.builder().fightId(fightId).predictedWinner(winner).confidenceScore(confidence).cachedAt(OffsetDateTime.now()).build();
            var existing = mlPredictionRepository.findByFightId(fightId);
            existing.ifPresent(old -> p.setId(old.getId()));
            return mlPredictionRepository.save(p);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh ML prediction", e);
        }
    }
}
