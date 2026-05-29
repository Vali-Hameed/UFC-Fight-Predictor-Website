package com.valihameed.ufcfightpredictor.ml;

import com.valihameed.ufcfightpredictor.models.MlPrediction;
import com.valihameed.ufcfightpredictor.repository.MlPredictionRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@AllArgsConstructor
public class MlService {
    private final RestTemplate restTemplate;
    private final MlPredictionRepository mlPredictionRepository;

    @Value("${fastapi.base-url}")
    private String fastapiBase;

    public MlPrediction getPrediction(String fighter1, String fighter2, Long fightId) {
        // check cache
        return mlPredictionRepository.findByFightId(fightId).orElseGet(() -> {
            String url = String.format("%s/predict?fighter1=%s&fighter2=%s", fastapiBase, fighter1, fighter2);
            try {
                Map resp = restTemplate.getForObject(url, Map.class);
                String winner = (String) resp.get("predicted_winner");
                Double confidence = Double.valueOf(resp.get("confidence_score").toString());
                MlPrediction p = MlPrediction.builder().fightId(fightId).predictedWinner(winner).confidenceScore(confidence).cachedAt(OffsetDateTime.now()).build();
                return mlPredictionRepository.save(p);
            } catch (Exception e) {
                return null;
            }
        });
    }
}
